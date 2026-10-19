package com.littlehorizons.erp.model.academic;

/**
 * Materia de un grado ({@code materias}).
 *
 * @param id        {@code null} antes de insertar el registro
 * @param gradeName obtenido de {@code grados}; puede ser {@code null} al insertar
 */
public record Subject(
        Integer id,
        String code,
        String name,
        String description,
        int gradeId,
        String gradeName,
        int weeklyHours,
        boolean active) {

    public Subject withId(int newId) {
        return new Subject(newId, code, name, description, gradeId, gradeName, weeklyHours, active);
    }
}
