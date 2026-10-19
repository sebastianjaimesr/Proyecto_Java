package com.littlehorizons.erp.model.academic;

/**
 * Grado escolar del catálogo {@code grados} (Preescolar = 0, Primero a Once = 1..11).
 */
public record Grade(int id, String name, int level) {
}
