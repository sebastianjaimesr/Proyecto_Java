package com.littlehorizons.erp.model.finance;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de factura de un pago ({@code facturas}).
 *
 * @param number      consecutivo, por ejemplo FE-000123
 * @param issuedAt    fecha y hora de emisión, guardadas en {@code fecha_creacion} para
 *                    poder recalcular el CUFE simulado
 * @param cufe        CUFE simulado (SHA-384, 96 caracteres hexadecimales)
 * @param pdfFileName {@code null} mientras no se haya generado el PDF
 * @param pdfPath     {@code null} mientras no se haya generado el PDF
 */
public record Invoice(
        Integer id,
        String number,
        int paymentId,
        LocalDate issueDate,
        LocalDateTime issuedAt,
        String cufe,
        String pdfFileName,
        String pdfPath) {

    /** Indica si la factura ya tiene su PDF generado. */
    public boolean hasPdf() {
        return pdfPath != null;
    }
}
