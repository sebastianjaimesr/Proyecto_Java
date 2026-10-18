package com.littlehorizons.erp.model.academic;

import java.time.LocalDate;

/**
 * Daily attendance of a student in a subject ({@code asistencias}).
 *
 * @param id          {@code null} before the record is inserted
 * @param recordedBy  teacher who took attendance
 * @param subjectName joined, for display
 */
public record Attendance(
        Integer id,
        int studentId,
        int subjectId,
        LocalDate date,
        AttendanceStatus status,
        String notes,
        Integer recordedBy,
        String subjectName) {
}
