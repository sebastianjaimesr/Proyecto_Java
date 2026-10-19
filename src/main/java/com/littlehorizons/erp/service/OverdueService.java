package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.model.finance.OverdueObligation;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.util.Formats;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Obligaciones pendientes por más de un mes (regla aprobada D-07, inferida
 * del esquema oficial, que no tiene tabla de cuentas por cobrar).
 *
 * <p>Para cada estudiante con matrícula activa en el año:</p>
 * <ul>
 * <li>Conceptos que aplican: los de {@code finance.mandatory.concepts}
 *       (MAT y PENSION por defecto) más cualquier concepto recurrente que el estudiante
 *       ya haya pagado ese año (por ejemplo, transporte).</li>
 * <li>Concepto recurrente: vencido cuando el último pago, o la fecha de matrícula
 *       si nunca se pagó, tiene más de un mes.</li>
 * <li>Concepto no recurrente: vencido cuando lo pagado en el año es
 *       menor que el valor esperado y la matrícula tiene más de un mes.</li>
 * </ul>
 * <p>Los días de mora se cuentan desde el final de ese mes de gracia.</p>
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

    /** Obligaciones vencidas del año, primero las más atrasadas. */
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
