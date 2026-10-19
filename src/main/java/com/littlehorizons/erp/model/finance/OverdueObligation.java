package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Obligación pendiente por más de un mes (regla inferida D-07, ver
 * {@link com.littlehorizons.erp.service.OverdueService}).
 *
 * @param referenceDate último pago del concepto, o la fecha de matrícula si
 *                      nunca se pagó
 * @param pendingAmount valor esperado menos lo pagado en el período actual
 */
public record OverdueObligation(
        int studentId,
        String studentDocument,
        String studentName,
        String gradeName,
        String conceptCode,
        String conceptName,
        LocalDate referenceDate,
        long daysOverdue,
        BigDecimal pendingAmount,
        String guardianName,
        String guardianPhone) {
}
