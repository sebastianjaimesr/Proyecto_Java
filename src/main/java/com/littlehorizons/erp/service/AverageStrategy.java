package com.littlehorizons.erp.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Algorithm used to combine several grades into one (Strategy pattern).
 *
 * <p>The statement does not define weights, so the application uses
 * {@link ArithmeticMeanStrategy}. A weighted rule approved by the instructor
 * would be a new implementation, without changing the services (Open/Closed).</p>
 */
@FunctionalInterface
public interface AverageStrategy {

    /**
     * @param values grades between 0.0 and 5.0
     * @return the combined grade with one decimal, or {@code null} when there are no values
     */
    BigDecimal average(List<BigDecimal> values);
}
