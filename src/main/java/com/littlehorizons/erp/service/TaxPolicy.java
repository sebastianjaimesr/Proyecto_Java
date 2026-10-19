package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.finance.PaymentConcept;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * Regla de IVA que se aplica a las facturas (patrón Strategy).
 *
 * <p>El monto pagado es el total. Cuando un concepto tiene una tarifa de IVA configurada
 * ({@code finance.tax.percent.CODIGO}), la factura divide ese total en
 * subtotal e IVA; si no, el IVA se muestra como "no aplica". El proyecto no
 * decide qué servicios del colegio están gravados: las tarifas son configuración.</p>
 */
public interface TaxPolicy {

    /** Porcentaje de IVA (0 a 100) de un concepto. */
    BigDecimal ratePercent(PaymentConcept concept);

    /** Divide un total que ya incluye el IVA. */
    default Breakdown split(PaymentConcept concept, BigDecimal total) {
        BigDecimal rate = ratePercent(concept);
        if (rate.signum() == 0) {
            return new Breakdown(total, BigDecimal.ZERO.setScale(2), rate, total);
        }
        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal subtotal = total.divide(divisor, 2, RoundingMode.HALF_UP);
        return new Breakdown(subtotal, total.subtract(subtotal), rate, total);
    }

    /** Subtotal + impuesto = total, con dos decimales. */
    record Breakdown(BigDecimal subtotal, BigDecimal tax, BigDecimal ratePercent, BigDecimal total) {
    }

    /** Tarifas de la configuración por código de concepto; los conceptos sin tarifa no tienen IVA. */
    static TaxPolicy configured(Map<String, BigDecimal> ratesByCode) {
        Map<String, BigDecimal> rates = Map.copyOf(Objects.requireNonNull(ratesByCode, "ratesByCode"));
        return concept -> rates.getOrDefault(concept.code(), BigDecimal.ZERO);
    }
}
