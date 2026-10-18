package com.littlehorizons.erp.model.academic;

import java.time.LocalDate;

/**
 * Yearly enrollment of a student in a grade ({@code matriculas}).
 *
 * @param id        {@code null} before the record is inserted
 * @param gradeName joined from {@code grados}; may be {@code null} on insert
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
