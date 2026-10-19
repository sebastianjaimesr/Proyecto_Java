package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Todo lo que se imprime en el PDF de una factura, ya calculado. El generador de PDF
 * solo lo acomoda en la página.
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

    /** Una línea de detalle: descripción, cantidad, valor unitario (antes de impuestos) y subtotal de la línea. */
    public record Line(String description, int quantity, BigDecimal unitValue, BigDecimal subtotal) {
    }
}
