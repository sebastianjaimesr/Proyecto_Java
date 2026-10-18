package com.littlehorizons.erp.model.finance;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Invoice record of a payment ({@code facturas}).
 *
 * @param number      consecutive such as FE-000123
 * @param issuedAt    issue date and time, stored in {@code fecha_creacion} so
 *                    the simulated CUFE can be recomputed
 * @param cufe        simulated CUFE (SHA-384, 96 hexadecimal characters)
 * @param pdfFileName {@code null} while the PDF has not been generated
 * @param pdfPath     {@code null} while the PDF has not been generated
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

    public boolean hasPdf() {
        return pdfPath != null;
    }
}
