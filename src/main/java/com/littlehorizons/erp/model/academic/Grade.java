package com.littlehorizons.erp.model.academic;

/**
 * School grade from the {@code grados} catalogue (Preescolar = 0, Primero to Once = 1..11).
 */
public record Grade(int id, String name, int level) {
}
