package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment received ({@code pagos}).
 *
 * @param id            {@code null} before the record is inserted
 * @param guardianId    guardian who paid; may be {@code null}
 * @param receiptNumber unique receipt; generated when the user does not type one
 * @param academicYear  year the payment applies to; may be {@code null}
 * @param period        period the payment applies to; may be {@code null}
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
