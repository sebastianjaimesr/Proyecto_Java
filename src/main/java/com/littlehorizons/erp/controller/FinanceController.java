package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.finance.ChargeStatus;
import com.littlehorizons.erp.model.finance.Invoice;
import com.littlehorizons.erp.model.finance.OverdueObligation;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;
import com.littlehorizons.erp.service.InvoiceService;
import com.littlehorizons.erp.service.OverdueService;
import com.littlehorizons.erp.service.PaymentConceptService;
import com.littlehorizons.erp.service.PaymentService;
import com.littlehorizons.erp.util.Formats;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates "Gestión financiera": concepts, adjustments, payments, invoices
 * and overdue obligations.
 */
public class FinanceController extends BaseController {

    private final PaymentConceptService conceptService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final OverdueService overdueService;

    public FinanceController(PaymentConceptService conceptService, PaymentService paymentService,
                             InvoiceService invoiceService, OverdueService overdueService) {
        this.conceptService = Objects.requireNonNull(conceptService, "conceptService");
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
        this.invoiceService = Objects.requireNonNull(invoiceService, "invoiceService");
        this.overdueService = Objects.requireNonNull(overdueService, "overdueService");
    }

    public OperationResult<List<PaymentConcept>> findConcepts(boolean onlyActive) {
        return execute(() -> conceptService.findConcepts(onlyActive), list -> list.size() + " concepto(s).");
    }

    public OperationResult<PaymentConcept> createConcept(PaymentConcept draft) {
        return execute(() -> conceptService.create(draft), concept -> "Concepto " + concept.code() + " creado.");
    }

    public OperationResult<PaymentConcept> updateConcept(PaymentConcept changes) {
        return execute(() -> conceptService.update(changes), "Concepto actualizado.");
    }

    public OperationResult<PriceAdjustment> addAdjustment(PriceAdjustment draft) {
        return execute(() -> conceptService.addAdjustment(draft),
                adjustment -> adjustment.type().dbValue() + " registrado para " + adjustment.conceptName() + ".");
    }

    public OperationResult<Boolean> removeAdjustment(int adjustmentId) {
        return execute(() -> {
            conceptService.removeAdjustment(adjustmentId);
            return Boolean.TRUE;
        }, "Ajuste eliminado. Los pagos registrados no cambian.");
    }

    public OperationResult<List<PriceAdjustment>> findAdjustments(int studentId, int academicYear) {
        return execute(() -> conceptService.findAdjustments(studentId, academicYear), list -> list.size() + " ajuste(s).");
    }

    public OperationResult<BigDecimal> expectedAmount(int studentId, int conceptId, int academicYear) {
        return execute(() -> conceptService.expectedAmount(studentId, conceptId, academicYear),
                amount -> "Valor a pagar: " + Formats.money(amount) + ".");
    }

    public OperationResult<PaymentService.PaymentReceipt> registerPayment(PaymentService.PaymentRequest request,
                                                                         boolean confirmDuplicate) {
        return execute(() -> paymentService.register(request, confirmDuplicate),
                receipt -> "Pago registrado con recibo " + receipt.payment().receiptNumber() + " y factura "
                        + receipt.invoice().number() + ".");
    }

    public OperationResult<List<Payment>> historyByStudent(int studentId, Integer academicYear) {
        return execute(() -> paymentService.historyByStudent(studentId, academicYear), list -> list.size() + " pago(s).");
    }

    public OperationResult<List<Payment>> historyByGuardian(int guardianId) {
        return execute(() -> paymentService.historyByGuardian(guardianId), list -> list.size() + " pago(s).");
    }

    public OperationResult<List<ChargeStatus>> statement(int studentId, int academicYear) {
        return execute(() -> paymentService.statement(studentId, academicYear), "Estado de cuenta calculado.");
    }

    public OperationResult<List<OverdueObligation>> overdue(int academicYear) {
        return execute(() -> overdueService.findOverdue(academicYear),
                list -> list.size() + " obligación(es) con más de un mes de vencimiento.");
    }

    public OperationResult<List<Invoice>> pendingInvoices() {
        return execute(invoiceService::findPending, list -> list.size() + " factura(s) sin PDF.");
    }

    public OperationResult<List<InvoiceService.PdfResult>> regeneratePending() {
        return execute(invoiceService::regeneratePending, list -> list.stream()
                .filter(InvoiceService.PdfResult::generated).count() + " de " + list.size() + " PDF generado(s).");
    }
}
