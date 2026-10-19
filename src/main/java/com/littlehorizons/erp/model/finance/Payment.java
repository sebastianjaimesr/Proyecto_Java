package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago recibido ({@code pagos}).
 *
 * @param id            {@code null} antes de insertar el registro
 * @param guardianId    acudiente que pagó; puede ser {@code null}
 * @param receiptNumber recibo único; se genera cuando el usuario no digita uno
 * @param academicYear  año al que aplica el pago; puede ser {@code null}
 * @param period        período al que aplica el pago; puede ser {@code null}
 */
public record Payment(
        Integer id,
        int studentId,
        String studentName,
        Integer guardianId,
        String guardianName,
        int conceptId,
        String conceptCode,
        String conceptName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        String receiptNumber,
        Integer academicYear,
        PaymentPeriod period,
        String notes) {
}
