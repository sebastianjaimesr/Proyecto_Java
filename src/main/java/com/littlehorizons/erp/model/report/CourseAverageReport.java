package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Promedio de una materia para cada estudiante del curso (grado y sección
 * opcional) en un año.
 *
 * @param students      promedio de la materia de cada estudiante con notas
 * @param courseAverage media de los promedios de los estudiantes; {@code null} si no hay notas
 */
public record CourseAverageReport(
        int subjectId,
        String subjectName,
        String gradeName,
        int academicYear,
        String section,
        List<StudentGrade> students,
        BigDecimal courseAverage,
        BigDecimal highest,
        BigDecimal lowest) {

    public CourseAverageReport {
        students = List.copyOf(students);
    }

    /** Promedio de un estudiante en la materia. */
    public record StudentGrade(int studentId, String studentName, BigDecimal average, boolean finalGrade) {
    }
}
