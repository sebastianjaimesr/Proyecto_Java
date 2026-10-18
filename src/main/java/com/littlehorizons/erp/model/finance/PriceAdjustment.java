package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;

/**
 * Per-student adjustment of a concept for one year
 * ({@code valores_estudiante_concepto}, authorized change P-2).
 *
 * @param id              {@code null} before the record is inserted
 * @param discountPercent percentage for scholarships and sibling discounts
 * @param customAmount    value that replaces the base amount
 */
public record PriceAdjustment(
        Integer id,
        int studentId,
        int conceptId,
        String conceptName,
        int academicYear,
        AdjustmentType type,
        BigDecimal discountPercent,
        BigDecimal customAmount,
        String notes) {
}
