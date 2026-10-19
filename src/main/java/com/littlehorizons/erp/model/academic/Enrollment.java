package com.littlehorizons.erp.model.academic;

import java.time.LocalDate;

/**
 * Matrícula anual de un estudiante en un grado ({@code matriculas}).
 *
 * @param id        {@code null} antes de insertar el registro
 * @param gradeName obtenido de {@code grados}; puede ser {@code null} al insertar
 */
public record Enrollment(
        Integer id,
        int studentId,
        int gradeId,
        String gradeName,
        int academicYear,
        EnrollmentStatus status,
        LocalDate enrollmentDate,
        LocalDate withdrawalDate,
        String notes) {
}
