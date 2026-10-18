package com.littlehorizons.erp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Simple arithmetic mean rounded to one decimal with HALF_UP (3.45 becomes
 * 3.5), the same precision as the DECIMAL(2,1) column.
 */
public class ArithmeticMeanStrategy implements AverageStrategy {

    public static final int SCALE = 1;

    @Override
    public BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
    }
}
