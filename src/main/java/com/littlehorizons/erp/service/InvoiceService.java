package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.finance.Invoice;
import com.littlehorizons.erp.model.finance.InvoiceDocument;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.InvoicePdfGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Numeración de facturas, CUFE y QR simulados, y generación del PDF.
 *
 * <p>Diseño de recuperación: la fila de la factura (número y CUFE) se crea en la misma
 * transacción que el pago. El PDF se genera después, fuera de cualquier
 * transacción, porque escribir un archivo es lento y no se puede deshacer. Si
 * el PDF falla, el pago y la factura quedan registrados con
 * {@code ruta_archivo = NULL}; la opción del menú "Regenerar facturas
 * pendientes" vuelve a construir el PDF con los datos guardados, sin crear un
 * pago nuevo.</p>
 */
public class InvoiceService {

    private static final DateTimeFormatter QR_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final TransactionManager transactions;
    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final PaymentConceptDao conceptDao;
    private final GuardianDao guardianDao;
    private final TaxPolicy taxPolicy;
    private final FinanceSettings settings;
    private final InvoicePdfGenerator pdfGenerator;

    public InvoiceService(TransactionManager transactions, InvoiceDao invoiceDao, PaymentDao paymentDao,
                          PaymentConceptDao conceptDao, GuardianDao guardianDao, TaxPolicy taxPolicy,
                          FinanceSettings settings, InvoicePdfGenerator pdfGenerator) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.invoiceDao = Objects.requireNonNull(invoiceDao, "invoiceDao");
        this.paymentDao = Objects.requireNonNull(paymentDao, "paymentDao");
        this.conceptDao = Objects.requireNonNull(conceptDao, "conceptDao");
        this.guardianDao = Objects.requireNonNull(guardianDao, "guardianDao");
        this.taxPolicy = Objects.requireNonNull(taxPolicy, "taxPolicy");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.pdfGenerator = Objects.requireNonNull(pdfGenerator, "pdfGenerator");
    }

    /** "FE-000123" para la factura con id 123. */
    public static String invoiceNumber(int invoiceId) {
        return String.format("FE-%06d", invoiceId);
    }

    /** "factura_000123.pdf" para la factura con id 123. */
    public static String fileName(int invoiceId) {
        return String.format("factura_%06d.pdf", invoiceId);
    }

    /**
     * Crea la factura de un pago dentro de la transacción de quien la llama. El
     * consecutivo sale del id AUTO_INCREMENT, así es único sin
     * bloqueos adicionales.
     */
    Invoice createInvoice(Connection connection, Payment payment, LocalDateTime issuedAt) throws SQLException {
        if (invoiceDao.findByPayment(connection, payment.id()).isPresent()) {
            throw new BusinessRuleException("El pago ya tiene una factura.");
        }
        int id = invoiceDao.insertDraft(connection, payment.id(), issuedAt.toLocalDate(), issuedAt);
        String number = invoiceNumber(id);
        String cufe = CufeGenerator.generate(number, settings.issuer().nit(), issuedAt, payment.amount());
        invoiceDao.assignNumberAndCufe(connection, id, number, cufe);
        return invoiceDao.findById(connection, id).orElseThrow();
    }

    /** Resultado de generar un PDF. */
    public record PdfResult(Invoice invoice, Path path, String errorMessage) {

        public boolean generated() {
            return path != null;
        }
    }

    /**
     * Genera el PDF de una factura y guarda su ruta. Los errores se registran
     * y se devuelven como mensaje; nunca deshacen el pago.
     */
    public PdfResult generatePdf(int invoiceId) {
        InvoiceData data = transactions.read(connection -> loadData(connection, invoiceId));
        Path target = settings.invoicesDirectory().resolve(fileName(invoiceId));
        Path written;
        try {
            written = pdfGenerator.generate(toDocument(data), target);
        } catch (IOException | RuntimeException e) {
            AppLogger.logError(InvoiceService.class, "Invoice PDF generation failed for " + data.invoice().number(), e);
            return new PdfResult(data.invoice(), null, "El pago quedó registrado, pero no se pudo generar el PDF de la "
                    + "factura " + data.invoice().number() + ". Use la opción \"Regenerar facturas pendientes\".");
        }
        String path = written.toString();
        transactions.inTransactionVoid(connection ->
                invoiceDao.setPdfFile(connection, invoiceId, fileName(invoiceId), path));
        Invoice updated = transactions.read(connection -> invoiceDao.findById(connection, invoiceId).orElseThrow());
        return new PdfResult(updated, written, null);
    }

    /** Reintenta todas las facturas a las que les falta el PDF. */
    public List<PdfResult> regeneratePending() {
        List<Invoice> pending = transactions.read(invoiceDao::findWithoutPdf);
        return pending.stream().map(invoice -> generatePdf(invoice.id())).toList();
    }

    public List<Invoice> findPending() {
        return transactions.read(invoiceDao::findWithoutPdf);
    }

    public Invoice findByPayment(int paymentId) {
        return transactions.read(connection -> ServiceChecks.require(invoiceDao.findByPayment(connection, paymentId),
                "El pago " + paymentId + " no tiene factura."));
    }

    /** Construye el documento imprimible; visible en el paquete para las pruebas. */
    InvoiceDocument toDocument(InvoiceData data) {
        Payment payment = data.payment();
        Invoice invoice = data.invoice();
        TaxPolicy.Breakdown breakdown = taxPolicy.split(data.concept(), payment.amount());
        String description = data.concept().name() + periodText(payment);
        InvoiceDocument.Line line = new InvoiceDocument.Line(description, 1, breakdown.subtotal(), breakdown.subtotal());
        Guardian guardian = data.guardian();
        String qr = "FACTURA:" + invoice.number()
                + "|FECHA:" + QR_DATE.format(invoice.issuedAt())
                + "|TOTAL:" + breakdown.total().toPlainString()
                + "|NIT:" + settings.issuer().nit()
                + "|CUFE:" + invoice.cufe();
        return new InvoiceDocument(invoice.number(), invoice.issuedAt(), settings.issuer(),
                guardian == null ? "Acudiente no registrado" : guardian.fullName(),
                guardian == null ? "-" : guardian.documentType().dbValue() + " " + guardian.documentNumber(),
                guardian == null ? null : guardian.address(),
                payment.studentName(), payment.receiptNumber(), payment.method().dbValue(), List.of(line),
                breakdown.subtotal(), breakdown.ratePercent(), breakdown.tax(), breakdown.total(), invoice.cufe(), qr);
    }

    private static String periodText(Payment payment) {
        StringBuilder text = new StringBuilder();
        if (payment.academicYear() != null) {
            text.append(" - año ").append(payment.academicYear());
        }
        if (payment.period() != null) {
            text.append(" - período ").append(payment.period().dbValue());
        }
        return text.toString();
    }

    /** Datos guardados que se necesitan para imprimir una factura. */
    record InvoiceData(Invoice invoice, Payment payment, PaymentConcept concept, Guardian guardian) {
    }

    private InvoiceData loadData(Connection connection, int invoiceId) throws SQLException {
        Invoice invoice = ServiceChecks.require(invoiceDao.findById(connection, invoiceId),
                "No existe una factura con id " + invoiceId + ".");
        Payment payment = paymentDao.findById(connection, invoice.paymentId()).orElseThrow();
        PaymentConcept concept = conceptDao.findById(connection, payment.conceptId()).orElseThrow();
        Guardian guardian = payment.guardianId() == null ? null
                : guardianDao.findById(connection, payment.guardianId()).orElse(null);
        return new InvoiceData(invoice, payment, concept, guardian);
    }
}
