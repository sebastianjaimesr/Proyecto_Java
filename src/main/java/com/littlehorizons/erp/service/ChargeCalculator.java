package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.finance.AdjustmentType;
import com.littlehorizons.erp.model.finance.PaymentConcept;
import com.littlehorizons.erp.model.finance.PriceAdjustment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * Value a student must pay for a concept in a year.
 *
 * <p>Rule: start from the custom value when the student has one, otherwise
 * from {@code conceptos_pago.monto_base}; then subtract the scholarship and
 * sibling discount percentages added together, with a maximum of 100 %.</p>
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
