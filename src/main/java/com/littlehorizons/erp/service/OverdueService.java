package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.finance.OverdueObligation;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.model.common.FileReport;
import com.littlehorizons.erp.util.Formats;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Obligations pending for more than one month (approved rule D-07, inferred
 * from the official schema, which has no accounts receivable table).
 *
 * <p>For each student with an active enrollment in the year:</p>
 * <ul>
 *   <li>Concepts that apply: those in {@code finance.mandatory.concepts}
 *       (MAT and PENSION by default) plus any recurring concept the student
 *       has already paid that year (for example transport).</li>
 *   <li>Recurring concept: overdue when the last payment, or the enrollment
 *       date if it was never paid, is more than one month old.</li>
 *   <li>Non-recurring concept: overdue when the amount paid in the year is
 *       below the expected value and the enrollment is more than one month old.</li>
 * </ul>
 * <p>Days overdue are counted from the end of that month of grace.</p>
 */
public class OverdueService {

    private final TransactionManager transactions;
    private final PaymentDao paymentDao;
    private final PaymentConceptDao conceptDao;
    private final ChargeCalculator calculator;
    private final FinanceSettings settings;
    private final Clock clock;

    public OverdueService(TransactionManager transactions, PaymentDao paymentDao, PaymentConceptDao conceptDao,
                          ChargeCalculator calculator, FinanceSettings settings, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.paymentDao = Objects.requireNonNull(paymentDao, "paymentDao");
        this.conceptDao = Objects.requireNonNull(conceptDao, "conceptDao");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Exercise 9: one collection letter per student with overdue
     * obligations (several debts, one letter), in exports/cartas.
     */
    public FileReport writeCollectionLetters(int academicYear) {
        LocalDate today = LocalDate.now(clock);
        Map<Integer, List<OverdueObligation>> byStudent = findOverdue(academicYear).stream()
                .collect(Collectors.groupingBy(OverdueObligation::studentId, LinkedHashMap::new,
                        Collectors.toList()));
        // The report masks the document; the file name needs the real one.
        Map<Integer, String> documents = transactions.read(connection ->
                paymentDao.findBillableStudents(connection, academicYear)).stream()
                .collect(Collectors.toMap(PaymentDao.BillableStudent::studentId, PaymentDao.BillableStudent::document,
                        (first, second) -> first));
        if (byStudent.isEmpty()) {
            throw new BusinessRuleException("No hay obligaciones vencidas en "
                    + academicYear + "; no se generaron cartas.");
        }
        Path folder = FileSupport.EXPORTS.resolve("cartas");
        List<Path> files = new ArrayList<>();
        byStudent.forEach((studentId, debts) -> {
            OverdueObligation first = debts.get(0);
            String document = documents.getOrDefault(studentId, String.valueOf(studentId));
            List<String> lines = new ArrayList<>();
            lines.add("COLEGIO INTERNACIONAL LITTLE HORIZONS - Tesorería");
            lines.add(Formats.longDate(today));
            lines.add("");
            lines.add("Señor(a) " + Formats.text(first.guardianName()));
            lines.add("Acudiente de " + first.studentName() + " (" + Formats.text(first.gradeName()) + ")");
            lines.add("");
            lines.add("Le recordamos que las siguientes obligaciones del año " + academicYear + " están vencidas:");
            lines.add("");
            lines.add(String.format("  %-28s %12s %16s", "Concepto", "Días de mora", "Pendiente"));
            debts.forEach(debt -> lines.add(String.format("  %-28s %12d %16s", debt.conceptName(),
                    debt.daysOverdue(), Formats.money(debt.pendingAmount()))));
            BigDecimal total = debts.stream().map(OverdueObligation::pendingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add("  " + "-".repeat(58));
            lines.add(String.format("  %-28s %12s %16s", "TOTAL ADEUDADO", "", Formats.money(total)));
            lines.add("");
            lines.add("Si ya realizó el pago, por favor ignore esta comunicación.");
            files.add(FileSupport.write(folder.resolve("carta_" + document + ".txt"), lines, OverdueService.class));
        });
        return new FileReport(folder, files);
    }

    /** Overdue obligations of the year, most overdue first. */
    public List<OverdueObligation> findOverdue(int academicYear) {
        LocalDate today = LocalDate.now(clock);
        LocalDate cutoff = today.minusMonths(1);
        return transactions.read(connection -> {
            List<PaymentConcept> concepts = conceptDao.findAll(connection, false);
            Map<Integer, List<Payment>> paymentsByStudent = paymentDao.findByYear(connection, academicYear).stream()
                    .collect(Collectors.groupingBy(Payment::studentId));
            List<OverdueObligation> result = new ArrayList<>();
            for (PaymentDao.BillableStudent student : paymentDao.findBillableStudents(connection, academicYear)) {
                List<Payment> payments = paymentsByStudent.getOrDefault(student.studentId(), List.of());
                Set<Integer> paidConcepts = payments.stream().map(Payment::conceptId).collect(Collectors.toSet());
                List<PriceAdjustment> adjustments = adjustments(connection, student.studentId(), academicYear);
                for (PaymentConcept concept : concepts) {
                    boolean applies = settings.mandatoryConcepts().contains(concept.code())
                            || (concept.recurring() && paidConcepts.contains(concept.id()));
                    if (!applies) {
                        continue;
                    }
                    evaluate(student, concept, payments, adjustments, cutoff, today).ifPresent(result::add);
                }
            }
            result.sort(Comparator.comparingLong(OverdueObligation::daysOverdue).reversed()
                    .thenComparing(OverdueObligation::studentName));
            return result;
        });
    }

    private List<PriceAdjustment> adjustments(Connection connection, int studentId, int year) throws SQLException {
        return conceptDao.findAdjustments(connection, studentId, year);
    }

    private Optional<OverdueObligation> evaluate(PaymentDao.BillableStudent student, PaymentConcept concept,
                                                 List<Payment> payments, List<PriceAdjustment> adjustments,
                                                 LocalDate cutoff, LocalDate today) {
        BigDecimal expected = calculator.expectedAmount(concept, adjustments);
        if (expected.signum() == 0) {
            return Optional.empty();
        }
        List<Payment> conceptPayments = payments.stream()
                .filter(payment -> payment.conceptId() == concept.id())
                .collect(Collectors.toList());
        LocalDate reference;
        BigDecimal pending;
        if (concept.recurring()) {
            reference = conceptPayments.stream().map(Payment::paymentDate).max(LocalDate::compareTo)
                    .orElse(student.enrollmentDate());
            if (!reference.isBefore(cutoff)) {
                return Optional.empty();
            }
            pending = expected;
        } else {
            BigDecimal paid = conceptPayments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            reference = student.enrollmentDate();
            if (paid.compareTo(expected) >= 0 || !reference.isBefore(cutoff)) {
                return Optional.empty();
            }
            pending = expected.subtract(paid);
        }
        long days = ChronoUnit.DAYS.between(reference.plusMonths(1), today);
        return Optional.of(new OverdueObligation(student.studentId(), Formats.maskDocument(student.document()),
                student.name(), student.gradeName(), concept.code(), concept.name(), reference, days, pending,
                student.guardianName(), student.guardianPhone()));
    }
}
