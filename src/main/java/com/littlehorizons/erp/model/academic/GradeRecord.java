package com.littlehorizons.erp.model.academic;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nota de un estudiante en una materia y período ({@code registros_academicos}).
 * {@code value} es un DECIMAL(2,1) entre 0.0 y 5.0.
 *
 * @param id          {@code null} antes de insertar el registro
 * @param studentName obtenido con join, para mostrar
 * @param subjectName obtenido con join, para mostrar
 * @param teacherName obtenido con join, para mostrar
 */
public record GradeRecord(
        Integer id,
        int studentId,
        int subjectId,
        int teacherId,
        int academicYear,
        GradingPeriod period,
        BigDecimal value,
        String notes,
        LocalDate recordDate,
        String studentName,
        String subjectName,
        String teacherName) {
}
