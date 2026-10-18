package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;

/**
 * Payment concept of the catalogue ({@code conceptos_pago}).
 *
 * @param id         {@code null} before the record is inserted
 * @param baseAmount value in Colombian pesos, DECIMAL(10,2)
 * @param recurring  monthly charge such as the pension
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
