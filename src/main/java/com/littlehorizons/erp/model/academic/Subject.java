package com.littlehorizons.erp.model.academic;

/**
 * Subject of one grade ({@code materias}).
 *
 * @param id        {@code null} before the record is inserted
 * @param gradeName joined from {@code grados}; may be {@code null} on insert
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
