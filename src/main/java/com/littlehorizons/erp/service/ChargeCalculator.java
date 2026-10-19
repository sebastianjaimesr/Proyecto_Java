package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * Valor que un estudiante debe pagar por un concepto en un año.
 *
 * <p>Regla: se parte del valor personalizado si el estudiante tiene uno; si no,
 * de {@code conceptos_pago.monto_base}; luego se resta la suma de los porcentajes
 * de beca y de descuento por hermanos, con un máximo de 100 %.</p>
 */
public class ChargeCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public BigDecimal expectedAmount(PaymentConcept concept, Collection<PriceAdjustment> adjustments) {
        BigDecimal base = adjustments.stream()
                .filter(adjustment -> adjustment.conceptId() == concept.id())
                .filter(adjustment -> adjustment.type() == AdjustmentType.CUSTOM_VALUE)
                .map(PriceAdjustment::customAmount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(concept.baseAmount());
        BigDecimal discount = adjustments.stream()
                .filter(adjustment -> adjustment.conceptId() == concept.id())
                .filter(adjustment -> adjustment.type() != AdjustmentType.CUSTOM_VALUE)
                .map(PriceAdjustment::discountPercent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(HUNDRED);
        return base.multiply(HUNDRED.subtract(discount)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
