package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Obligation pending for more than one month (inferred rule D-07, see
 * {@link com.littlehorizons.erp.service.OverdueService}).
 *
 * @param referenceDate last payment of the concept, or the enrollment date when
 *                      it was never paid
 * @param pendingAmount expected minus paid for the current period
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
