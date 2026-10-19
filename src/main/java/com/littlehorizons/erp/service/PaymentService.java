package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfirmationRequiredException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.finance.ChargeStatus;
import com.littlehorizons.erp.model.finance.Invoice;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PaymentMethod;
import com.littlehorizons.erp.model.finance.PaymentPeriod;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registro de pagos con su factura, historial de pagos y estado
 * de cuenta (pagos parciales frente a completos).
 */
public class PaymentService {

    private static final java.util.regex.Pattern RECEIPT = java.util.regex.Pattern.compile("^[A-Z0-9][A-Z0-9_-]{2,49}$");

    private final TransactionManager transactions;
    private final PaymentDao paymentDao;
    private final PaymentConceptDao conceptDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final InvoiceService invoiceService;
    private final ChargeCalculator calculator;
    private final Clock clock;

    public PaymentService(TransactionManager transactions, PaymentDao paymentDao, PaymentConceptDao conceptDao,
                          StudentDao studentDao, GuardianDao guardianDao, InvoiceService invoiceService,
                          ChargeCalculator calculator, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.paymentDao = Objects.requireNonNull(paymentDao, "paymentDao");
        this.conceptDao = Objects.requireNonNull(conceptDao, "conceptDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.guardianDao = Objects.requireNonNull(guardianDao, "guardianDao");
        this.invoiceService = Objects.requireNonNull(invoiceService, "invoiceService");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Datos que digita el usuario para registrar un pago. */
    public record PaymentRequest(
            int studentId,
            Integer guardianId,
            int conceptId,
            BigDecimal amount,
            LocalDate paymentDate,
            PaymentMethod method,
            String receiptNumber,
            int academicYear,
            PaymentPeriod period,
            String notes) {
    }

    /** El pago, su factura y el resultado de generar el PDF. */
    public record PaymentReceipt(Payment payment, Invoice invoice, BigDecimal expectedAmount,
                                 InvoiceService.PdfResult pdf) {
    }

    /**
     * Registra un pago y su factura en una transacción y luego genera
     * el PDF fuera de la transacción.
     *
     * <p>Protección contra reintentos accidentales: un número de recibo digitado debe ser
     * único (UNIQUE numero_recibo), y un pago idéntico a uno existente
     * (mismo estudiante, concepto, monto, fecha y método) solo se acepta cuando
     * {@code confirmDuplicate} es true.</p>
     */
    public PaymentReceipt register(PaymentRequest request, boolean confirmDuplicate) {
        validate(request);
        String receipt = request.receiptNumber() == null || request.receiptNumber().isBlank()
                ? null : request.receiptNumber().strip().toUpperCase(java.util.Locale.ROOT);
        if (receipt != null && !RECEIPT.matcher(receipt).matches()) {
            throw new ValidationException("El número de recibo solo admite letras, números, '-' y '_' (3 a 50 caracteres).");
        }
        LocalDateTime issuedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Registered registered = transactions.inTransaction(connection -> {
            Student student = ServiceChecks.require(studentDao.findById(connection, request.studentId()),
                    "No existe un estudiante con id " + request.studentId() + ".");
            PaymentConcept concept = ServiceChecks.require(conceptDao.findById(connection, request.conceptId()),
                    "No existe un concepto con id " + request.conceptId() + ".");
            if (!concept.active()) {
                throw new BusinessRuleException("El concepto " + concept.name() + " está inactivo.");
            }
            Guardian payer = resolvePayer(connection, student, request.guardianId());
            if (receipt != null && paymentDao.findByReceipt(connection, receipt).isPresent()) {
                throw new BusinessRuleException("Ya existe un pago con el recibo " + receipt
                        + ". No se registró de nuevo.");
            }
            Optional<Payment> similar = paymentDao.findSimilar(connection, request.studentId(), request.conceptId(),
                    request.amount(), request.paymentDate(), request.method());
            if (similar.isPresent() && !confirmDuplicate) {
                throw new ConfirmationRequiredException("Ya existe un pago igual (recibo " + similar.get().receiptNumber()
                        + ", " + Formats.money(similar.get().amount()) + ", " + similar.get().paymentDate()
                        + "). Confirme solo si es un pago diferente.");
            }
            int paymentId = paymentDao.insert(connection, new Payment(null, student.id(), student.fullName(),
                    payer.id(), payer.fullName(), concept.id(), concept.code(), concept.name(), request.amount(),
                    request.paymentDate(), request.method(), receipt, request.academicYear(), request.period(),
                    InputValidator.optionalText(request.notes(), "notas", 1000)));
            if (receipt == null) {
                paymentDao.setReceiptNumber(connection, paymentId,
                        String.format("RC-%d-%06d", request.paymentDate().getYear(), paymentId));
            }
            Payment payment = paymentDao.findById(connection, paymentId).orElseThrow();
            Invoice invoice = invoiceService.createInvoice(connection, payment, issuedAt);
            BigDecimal expected = calculator.expectedAmount(concept,
                    conceptDao.findAdjustments(connection, student.id(), request.academicYear()));
            return new Registered(payment, invoice, expected);
        });
        InvoiceService.PdfResult pdf = invoiceService.generatePdf(registered.invoice().id());
        return new PaymentReceipt(registered.payment(), pdf.invoice(), registered.expected(), pdf);
    }

    /** Datos que se guardan dentro de la transacción: el pago, su factura y el valor esperado. */
    private record Registered(Payment payment, Invoice invoice, BigDecimal expected) {
    }

    /** Pagos de un estudiante; {@code academicYear} null significa todos los años. */
    public List<Payment> historyByStudent(int studentId, Integer academicYear) {
        return transactions.read(connection -> {
            ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            return paymentDao.findByStudent(connection, studentId, academicYear);
        });
    }

    /** Pagos hechos por un acudiente o para los estudiantes a su cargo. */
    public List<Payment> historyByGuardian(int guardianId) {
        return transactions.read(connection -> {
            ServiceChecks.require(guardianDao.findById(connection, guardianId),
                    "No existe un acudiente con id " + guardianId + ".");
            return paymentDao.findByGuardian(connection, guardianId);
        });
    }

    /**
     * Valor esperado frente a pagado por concepto. Los conceptos recurrentes se comparan por
     * mes de pago; los demás, por año. Solo se listan los conceptos con pagos
     * (los pendientes aparecen en el reporte de morosidad).
     */
    public List<ChargeStatus> statement(int studentId, int academicYear) {
        return transactions.read(connection -> {
            ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            List<Payment> payments = paymentDao.findByStudent(connection, studentId, academicYear);
            List<PriceAdjustment> adjustments = conceptDao.findAdjustments(connection, studentId, academicYear);
            Map<Integer, PaymentConcept> concepts = conceptDao.findAll(connection, false).stream()
                    .collect(Collectors.toMap(PaymentConcept::id, concept -> concept));
            return payments.stream()
                    .collect(Collectors.groupingBy(payment -> new ChargeKey(payment.conceptId(),
                            concepts.get(payment.conceptId()).recurring()
                                    ? payment.paymentDate().getMonthValue() : null)))
                    .entrySet().stream()
                    .map(entry -> toStatus(concepts.get(entry.getKey().conceptId()), academicYear,
                            entry.getKey().month(), entry.getValue(), adjustments))
                    .sorted(Comparator.comparing(ChargeStatus::conceptName)
                            .thenComparing(status -> status.month() == null ? 0 : status.month()))
                    .collect(Collectors.toList());
        });
    }

    /** Clave para agrupar pagos: concepto y mes (el mes es null en conceptos no recurrentes). */
    private record ChargeKey(int conceptId, Integer month) {
    }

    /** Compara lo esperado con lo pagado y arma el {@link ChargeStatus} (pago completo o parcial). */
    private ChargeStatus toStatus(PaymentConcept concept, int year, Integer month, List<Payment> payments,
                                  List<PriceAdjustment> adjustments) {
        BigDecimal expected = calculator.expectedAmount(concept, adjustments);
        BigDecimal paid = payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = expected.subtract(paid);
        ChargeStatus.State state = balance.signum() > 0 ? ChargeStatus.State.PARTIAL
                : balance.signum() == 0 ? ChargeStatus.State.PAID : ChargeStatus.State.OVERPAID;
        return new ChargeStatus(concept.id(), concept.code(), concept.name(), year, month, expected, paid,
                balance.max(BigDecimal.ZERO), state);
    }

    /**
     * Decide qué acudiente aparece como quien pagó: el indicado por el usuario (si está vinculado al
     * estudiante) o, si no se indicó, el responsable financiero principal.
     */
    private Guardian resolvePayer(Connection connection, Student student, Integer guardianId) throws SQLException {
        List<GuardianLink> links = guardianDao.findGuardiansOfStudent(connection, student.id());
        if (guardianId != null) {
            return links.stream().map(GuardianLink::guardian)
                    .filter(guardian -> guardian.id().equals(guardianId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("El acudiente " + guardianId
                            + " no está relacionado con el estudiante."));
        }
        return links.stream()
                .filter(link -> link.guardian().financialResponsible())
                .sorted(Comparator.comparing(GuardianLink::primary).reversed())
                .map(GuardianLink::guardian)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("El estudiante no tiene un acudiente responsable "
                        + "financiero. Registre uno antes de recibir pagos."));
    }

    /** Valida los datos del pago: método obligatorio, monto mayor que cero, fecha no futura y año académico válido. */
    private void validate(PaymentRequest request) {
        if (request.method() == null) {
            throw new ValidationException("El método de pago es obligatorio.");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new ValidationException("El monto pagado debe ser mayor que cero.");
        }
        InputValidator.parseMoney(request.amount().toPlainString(), "monto pagado");
        LocalDate today = LocalDate.now(clock);
        ServiceChecks.notInFuture(request.paymentDate(), today, "fecha de pago");
        ServiceChecks.academicYear(request.academicYear(), today);
    }
}
