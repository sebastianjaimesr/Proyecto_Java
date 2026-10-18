package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything printed on an invoice PDF, already computed. The PDF generator
 * only lays it out.
 */
public record InvoiceDocument(
        String number,
        LocalDateTime issuedAt,
        IssuerInfo issuer,
        String customerName,
        String customerDocument,
        String customerAddress,
        String studentName,
        String receiptNumber,
        String paymentMethod,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal taxRatePercent,
        BigDecimal tax,
        BigDecimal total,
        String cufe,
        String qrContent) {

    public InvoiceDocument {
        lines = List.copyOf(lines);
    }

    /** One detail line: description, quantity, unit value (before tax) and line subtotal. */
    public record Line(String description, int quantity, BigDecimal unitValue, BigDecimal subtotal) {
    }
}
