package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcCatalogDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcInvoiceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcPaymentConceptDao;
import com.littlehorizons.erp.dao.jdbc.JdbcPaymentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfirmationRequiredException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.ChargeStatus;
import com.littlehorizons.erp.model.finance.OverdueObligation;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PaymentMethod;
import com.littlehorizons.erp.model.finance.PaymentPeriod;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.support.TestDatabase;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.InvoicePdfGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Payments, invoices and overdue report against the MySQL test database.
 */
class FinanceIntegrationTest {

    private static final int YEAR = 2026;
    private static final int GRADE_THIRD = 4;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(900);

    @TempDir
    static Path invoicesDir;

    private static TransactionManager transactions;
    private static StudentService students;
    private static GuardianService guardians;
    private static EnrollmentService enrollments;
    private static PaymentConceptService concepts;
    private static PaymentService payments;
    private static OverdueService overdue;
    private static FinanceSettings settings;
    private static Map<String, PaymentConcept> conceptsByCode;

    @BeforeAll
    static void createDatabase() {
        TestDatabase database = TestDatabase.recreate();
        transactions = database.transactionManager();
        JdbcStudentDao studentDao = new JdbcStudentDao();
        JdbcGuardianDao guardianDao = new JdbcGuardianDao();
        JdbcPaymentConceptDao conceptDao = new JdbcPaymentConceptDao();
        JdbcPaymentDao paymentDao = new JdbcPaymentDao();
        ChargeCalculator calculator = new ChargeCalculator();
        settings = FinanceSettings.defaults(invoicesDir);
        students = new StudentService(transactions, studentDao, guardianDao, new JdbcEnrollmentDao(), TestDatabase.CLOCK);
        guardians = new GuardianService(transactions, guardianDao, studentDao);
        enrollments = new EnrollmentService(transactions, new JdbcEnrollmentDao(), studentDao, new JdbcCatalogDao(),
                TestDatabase.CLOCK);
        concepts = new PaymentConceptService(transactions, conceptDao, studentDao, guardianDao, calculator,
                TestDatabase.CLOCK);
        InvoiceService invoices = new InvoiceService(transactions, new JdbcInvoiceDao(), paymentDao, conceptDao,
                guardianDao, TaxPolicy.configured(Map.of("UNIFORME", new BigDecimal("19"))), settings,
                new InvoicePdfGenerator());
        payments = new PaymentService(transactions, paymentDao, conceptDao, studentDao, guardianDao, invoices,
                calculator, TestDatabase.CLOCK);
        overdue = new OverdueService(transactions, paymentDao, conceptDao, calculator, settings, TestDatabase.CLOCK);
        conceptsByCode = concepts.findConcepts(false).stream()
                .collect(java.util.stream.Collectors.toMap(PaymentConcept::code, concept -> concept));
    }

    private static Student enrolledStudentWithGuardian(Guardian sharedGuardian) {
        int n = SEQUENCE.incrementAndGet();
        Student student = students.register(new Student(null, StudentDocumentType.TI, "TI" + (3_000_000 + n),
                new PersonName("Valentina", null, "Pago" + n, null), LocalDate.of(2017, 6, 1), Gender.FEMALE, null,
                null, null, null, null, null, LocalDate.of(2023, 1, 20), StudentStatus.ACTIVE));
        if (sharedGuardian == null) {
            guardians.registerForStudent(new Guardian(null, PersonDocumentType.CC, "CC" + (40_000_000 + n),
                    new PersonName("Andrea", null, "Responsable" + n, null), "Madre", "3151112233", null, null,
                    "Calle 45 # 12-30, Medellín", "Contadora", true), student.id(), true, true);
        } else {
            guardians.link(student.id(), sharedGuardian.id(), true, true);
        }
        enrollments.enroll(student.id(), GRADE_THIRD, YEAR, LocalDate.of(2026, 1, 20), null);
        return student;
    }

    private static PaymentService.PaymentRequest request(Student student, String code, String amount,
                                                         LocalDate date, String receipt) {
        return new PaymentService.PaymentRequest(student.id(), null, conceptsByCode.get(code).id(),
                new BigDecimal(amount), date, PaymentMethod.TRANSFER, receipt, YEAR, PaymentPeriod.ANNUAL, null);
    }

    @Test
    @DisplayName("A payment creates its invoice, simulated CUFE and PDF in invoices/factura_NNNNNN.pdf")
    void paymentCreatesInvoiceAndPdf() throws IOException {
        Student student = enrolledStudentWithGuardian(null);

        PaymentService.PaymentReceipt receipt = payments.register(
                request(student, "PENSION", "350000", LocalDate.of(2026, 10, 1), null), false);

        assertTrue(receipt.payment().receiptNumber().startsWith("RC-2026-"));
        assertEquals(InvoiceService.invoiceNumber(receipt.invoice().id()), receipt.invoice().number());
        assertEquals(CufeGenerator.generate(receipt.invoice().number(), settings.issuer().nit(),
                receipt.invoice().issuedAt(), new BigDecimal("350000.00")), receipt.invoice().cufe(),
                "CUFE can be recomputed from stored data");
        assertTrue(receipt.pdf().generated());
        Path pdf = receipt.pdf().path();
        assertEquals(InvoiceService.fileName(receipt.invoice().id()), pdf.getFileName().toString());
        assertTrue(Files.size(pdf) > 1000);
        assertEquals(pdf.toString(), receipt.invoice().pdfPath());
        assertNotNull(receipt.payment().guardianName(), "the financial guardian is the customer");
    }

    @Test
    @DisplayName("If the PDF fails the payment is kept and the PDF can be regenerated without a new payment")
    void pdfFailureIsRecoverable() throws IOException {
        Path blocker = Files.writeString(invoicesDir.resolve("blocked-" + SEQUENCE.incrementAndGet()), "not a folder");
        JdbcPaymentDao paymentDao = new JdbcPaymentDao();
        JdbcPaymentConceptDao conceptDao = new JdbcPaymentConceptDao();
        JdbcGuardianDao guardianDao = new JdbcGuardianDao();
        FinanceSettings broken = FinanceSettings.defaults(blocker);
        InvoiceService brokenInvoices = new InvoiceService(transactions, new JdbcInvoiceDao(), paymentDao, conceptDao,
                guardianDao, TaxPolicy.configured(Map.of()), broken, new InvoicePdfGenerator());
        PaymentService brokenPayments = new PaymentService(transactions, paymentDao, conceptDao, new JdbcStudentDao(),
                guardianDao, brokenInvoices, new ChargeCalculator(), TestDatabase.CLOCK);
        Student student = enrolledStudentWithGuardian(null);

        PaymentService.PaymentReceipt receipt = brokenPayments.register(
                request(student, "ACTIVIDAD", "50000", LocalDate.of(2026, 4, 4), null), false);

        assertFalse(receipt.pdf().generated());
        assertTrue(receipt.pdf().errorMessage().contains("Regenerar"));
        assertEquals(1, payments.historyByStudent(student.id(), YEAR).size(), "the payment was saved");
        InvoiceService workingInvoices = new InvoiceService(transactions, new JdbcInvoiceDao(), paymentDao, conceptDao,
                guardianDao, TaxPolicy.configured(Map.of()), settings, new InvoicePdfGenerator());
        assertTrue(workingInvoices.findPending().stream().anyMatch(item -> item.id().equals(receipt.invoice().id())));

        List<InvoiceService.PdfResult> regenerated = workingInvoices.regeneratePending();

        assertTrue(regenerated.stream().allMatch(InvoiceService.PdfResult::generated));
        assertTrue(workingInvoices.findPending().isEmpty());
        assertEquals(1, payments.historyByStudent(student.id(), YEAR).size(), "no duplicated payment");
        assertEquals(receipt.invoice().number(), workingInvoices.findByPayment(receipt.payment().id()).number());
    }

    @Test
    @DisplayName("Accidental retries do not register the same payment twice")
    void duplicatePaymentsNeedConfirmationAndReceiptsAreUnique() {
        Student student = enrolledStudentWithGuardian(null);
        String receipt = "TRF-" + SEQUENCE.incrementAndGet();
        payments.register(request(student, "UNIFORME", "250000", LocalDate.of(2026, 3, 3), receipt), false);

        assertThrows(BusinessRuleException.class, () -> payments.register(
                request(student, "UNIFORME", "250000", LocalDate.of(2026, 3, 3), receipt), true));
        assertThrows(ConfirmationRequiredException.class, () -> payments.register(
                request(student, "UNIFORME", "250000", LocalDate.of(2026, 3, 3), null), false));
        PaymentService.PaymentReceipt second = payments.register(
                request(student, "UNIFORME", "250000", LocalDate.of(2026, 3, 3), null), true);

        assertEquals(2, payments.historyByStudent(student.id(), YEAR).size());
        assertNotNull(second.invoice().number());
    }

    @Test
    void invalidPaymentsAreRejected() {
        Student student = enrolledStudentWithGuardian(null);
        assertThrows(ValidationException.class, () -> payments.register(
                request(student, "MAT", "0", LocalDate.of(2026, 2, 1), null), false));
        assertThrows(ValidationException.class, () -> payments.register(
                request(student, "MAT", "-5", LocalDate.of(2026, 2, 1), null), false));
        assertThrows(ValidationException.class, () -> payments.register(
                request(student, "MAT", "100", TestDatabase.TODAY.plusDays(1), null), false));
        assertThrows(ValidationException.class, () -> payments.register(
                request(student, "MAT", "100", LocalDate.of(2026, 2, 1), "con espacios"), false));
    }

    @Test
    void paymentWithoutFinancialGuardianIsRejectedAndNothingIsSaved() {
        int n = SEQUENCE.incrementAndGet();
        Student orphan = students.register(new Student(null, StudentDocumentType.TI, "TI" + (3_500_000 + n),
                new PersonName("Sin", null, "Acudiente" + n, null), LocalDate.of(2016, 1, 1), Gender.MALE, null, null,
                null, null, null, null, LocalDate.of(2022, 1, 20), StudentStatus.ACTIVE));

        assertThrows(BusinessRuleException.class, () -> payments.register(
                request(orphan, "MAT", "500000", LocalDate.of(2026, 2, 1), null), false));
        assertTrue(payments.historyByStudent(orphan.id(), null).isEmpty());
    }

    @Test
    void vatIsOnlyAppliedWhenConfigured() {
        TaxPolicy policy = TaxPolicy.configured(Map.of("UNIFORME", new BigDecimal("19")));
        TaxPolicy.Breakdown uniform = policy.split(conceptsByCode.get("UNIFORME"), new BigDecimal("119000.00"));
        assertEquals(new BigDecimal("100000.00"), uniform.subtotal());
        assertEquals(new BigDecimal("19000.00"), uniform.tax());
        TaxPolicy.Breakdown pension = policy.split(conceptsByCode.get("PENSION"), new BigDecimal("350000.00"));
        assertEquals(0, pension.tax().signum());
        assertEquals(new BigDecimal("350000.00"), pension.subtotal());
    }

    @Test
    void scholarshipsSiblingDiscountsAndCustomValuesChangeTheExpectedAmount() {
        Student first = enrolledStudentWithGuardian(null);
        Guardian shared = guardians.findGuardiansOfStudent(first.id()).get(0).guardian();
        Student sibling = enrolledStudentWithGuardian(shared);
        Student onlyChild = enrolledStudentWithGuardian(null);
        int pension = conceptsByCode.get("PENSION").id();
        int registration = conceptsByCode.get("MAT").id();

        concepts.addAdjustment(new PriceAdjustment(null, sibling.id(), pension, null, YEAR,
                AdjustmentType.SIBLING_DISCOUNT, new BigDecimal("10"), null, null));
        concepts.addAdjustment(new PriceAdjustment(null, sibling.id(), pension, null, YEAR,
                AdjustmentType.SCHOLARSHIP, new BigDecimal("20"), null, "Beca académica"));
        concepts.addAdjustment(new PriceAdjustment(null, first.id(), registration, null, YEAR,
                AdjustmentType.CUSTOM_VALUE, null, new BigDecimal("420000"), "Convenio"));

        // 350000 * (100 - 30) / 100
        assertEquals(new BigDecimal("245000.00"), concepts.expectedAmount(sibling.id(), pension, YEAR));
        assertEquals(new BigDecimal("420000.00"), concepts.expectedAmount(first.id(), registration, YEAR));
        assertEquals(new BigDecimal("350000.00"), concepts.expectedAmount(first.id(), pension, YEAR));
        assertThrows(BusinessRuleException.class, () -> concepts.addAdjustment(new PriceAdjustment(null,
                onlyChild.id(), pension, null, YEAR, AdjustmentType.SIBLING_DISCOUNT, new BigDecimal("10"), null, null)));
        assertThrows(BusinessRuleException.class, () -> concepts.addAdjustment(new PriceAdjustment(null, sibling.id(),
                pension, null, YEAR, AdjustmentType.SCHOLARSHIP, new BigDecimal("5"), null, null)), "duplicate");
        assertThrows(ValidationException.class, () -> concepts.addAdjustment(new PriceAdjustment(null, sibling.id(),
                registration, null, YEAR, AdjustmentType.SCHOLARSHIP, new BigDecimal("150"), null, null)));
    }

    @Test
    void statementDistinguishesPartialAndCompletePayments() {
        Student student = enrolledStudentWithGuardian(null);
        payments.register(request(student, "MAT", "300000", LocalDate.of(2026, 1, 20), null), false);
        payments.register(request(student, "PENSION", "350000", LocalDate.of(2026, 2, 5), null), false);

        List<ChargeStatus> statement = payments.statement(student.id(), YEAR);

        ChargeStatus registration = statement.stream().filter(item -> item.conceptCode().equals("MAT")).findFirst()
                .orElseThrow();
        assertEquals(ChargeStatus.State.PARTIAL, registration.state());
        assertEquals(new BigDecimal("200000.00"), registration.balance());
        assertNull(registration.month());
        ChargeStatus february = statement.stream().filter(item -> item.conceptCode().equals("PENSION")).findFirst()
                .orElseThrow();
        assertEquals(ChargeStatus.State.PAID, february.state());
        assertEquals(2, february.month());
    }

    @Test
    @DisplayName("Overdue report: pension unpaid for more than a month and incomplete enrollment fee")
    void overdueObligationsAreInferred() {
        Student late = enrolledStudentWithGuardian(null);
        payments.register(request(late, "MAT", "500000", LocalDate.of(2026, 1, 20), null), false);
        payments.register(request(late, "PENSION", "350000", LocalDate.of(2026, 8, 10), null), false);
        Student upToDate = enrolledStudentWithGuardian(null);
        payments.register(request(upToDate, "MAT", "500000", LocalDate.of(2026, 1, 20), null), false);
        payments.register(request(upToDate, "PENSION", "350000", LocalDate.of(2026, 10, 1), null), false);
        Student partial = enrolledStudentWithGuardian(null);
        payments.register(request(partial, "MAT", "100000", LocalDate.of(2026, 1, 20), null), false);
        payments.register(request(partial, "PENSION", "350000", LocalDate.of(2026, 10, 2), null), false);

        List<OverdueObligation> report = overdue.findOverdue(YEAR);

        OverdueObligation pension = report.stream()
                .filter(row -> row.studentId() == late.id() && row.conceptCode().equals("PENSION"))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 8, 10), pension.referenceDate());
        // 2026-09-10 + 35 days = 2026-10-15
        assertEquals(35, pension.daysOverdue());
        assertNotNull(pension.guardianPhone());
        assertTrue(report.stream().noneMatch(row -> row.studentId() == late.id() && row.conceptCode().equals("MAT")));
        assertTrue(report.stream().noneMatch(row -> row.studentId() == upToDate.id()));
        OverdueObligation fee = report.stream()
                .filter(row -> row.studentId() == partial.id() && row.conceptCode().equals("MAT"))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("400000.00"), fee.pendingAmount());
        assertFalse(fee.studentDocument().startsWith("TI3"), "documents are masked in the report");
    }
}
