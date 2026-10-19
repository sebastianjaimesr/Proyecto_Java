package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Promedios de un estudiante en un año académico.
 *
 * @param subjects       una entrada por materia con inscripciones o notas
 * @param generalAverage media de los promedios de materia; las materias sin notas
 *                       se excluyen y se cuentan en {@code subjectsWithoutGrades}
 */
public record StudentAverageReport(
        int studentId,
        String studentName,
        int academicYear,
        String gradeName,
        List<SubjectAverage> subjects,
        BigDecimal generalAverage,
        long subjectsWithoutGrades) {

    public StudentAverageReport {
        subjects = List.copyOf(subjects);
    }
}
