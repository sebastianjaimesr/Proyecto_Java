package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;

/**
 * Ajuste de un concepto para un estudiante durante un año
 * ({@code valores_estudiante_concepto}, cambio autorizado P-2).
 *
 * @param id              {@code null} antes de insertar el registro
 * @param discountPercent porcentaje para becas y descuentos por hermanos
 * @param customAmount    valor que reemplaza el valor base
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
