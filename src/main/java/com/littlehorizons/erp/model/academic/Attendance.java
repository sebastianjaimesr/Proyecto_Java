package com.littlehorizons.erp.model.academic;

import java.time.LocalDate;

/**
 * Asistencia diaria de un estudiante en una materia ({@code asistencias}).
 *
 * @param id          {@code null} antes de insertar el registro
 * @param recordedBy  profesor que tomó la asistencia
 * @param subjectName obtenido con join, para mostrar
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
