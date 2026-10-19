package com.littlehorizons.erp.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Algoritmo que combina varias notas en una sola (patrón Strategy).
 *
 * <p>El enunciado no define pesos, así que la aplicación usa
 * {@link ArithmeticMeanStrategy}. Una regla ponderada aprobada por el instructor
 * sería una implementación nueva, sin cambiar los servicios (Abierto/Cerrado).</p>
 */
@FunctionalInterface
public interface AverageStrategy {

    /**
     * @param values notas entre 0.0 y 5.0
     * @return la nota combinada con un decimal, o {@code null} si no hay valores
     */
    BigDecimal average(List<BigDecimal> values);
}
