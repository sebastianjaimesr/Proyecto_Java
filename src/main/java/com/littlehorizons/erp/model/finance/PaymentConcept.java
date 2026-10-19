package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;

/**
 * Concepto de pago del catálogo ({@code conceptos_pago}).
 *
 * @param id         {@code null} antes de insertar el registro
 * @param baseAmount valor en pesos colombianos, DECIMAL(10,2)
 * @param recurring  cobro mensual, como la pensión
 */
public record PaymentConcept(
        Integer id,
        String code,
        String name,
        String description,
        BigDecimal baseAmount,
        boolean recurring,
        boolean active) {
}
