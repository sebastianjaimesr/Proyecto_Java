package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.finance.PaymentConcept;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * VAT rule applied to invoices (Strategy pattern).
 *
 * <p>The amount paid is the total. When a concept has a VAT rate configured
 * ({@code finance.tax.percent.CODE}), the invoice splits that total into
 * subtotal and VAT; otherwise VAT is shown as "no aplica". The project does
 * not decide which school services are taxed: the rates are configuration.</p>
 */
public interface TaxPolicy {

    /** VAT percentage (0 to 100) for a concept. */
    BigDecimal ratePercent(PaymentConcept concept);

    /** Splits a total that already includes VAT. */
    default Breakdown split(PaymentConcept concept, BigDecimal total) {
        BigDecimal rate = ratePercent(concept);
        if (rate.signum() == 0) {
            return new Breakdown(total, BigDecimal.ZERO.setScale(2), rate, total);
        }
        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal subtotal = total.divide(divisor, 2, RoundingMode.HALF_UP);
        return new Breakdown(subtotal, total.subtract(subtotal), rate, total);
    }

    /** Subtotal + tax = total, with two decimals. */
    record Breakdown(BigDecimal subtotal, BigDecimal tax, BigDecimal ratePercent, BigDecimal total) {
    }

    /** Rates from configuration by concept code; concepts without a rate have no VAT. */
    static TaxPolicy configured(Map<String, BigDecimal> ratesByCode) {
        Map<String, BigDecimal> rates = Map.copyOf(Objects.requireNonNull(ratesByCode, "ratesByCode"));
        return concept -> rates.getOrDefault(concept.code(), BigDecimal.ZERO);
    }
}
