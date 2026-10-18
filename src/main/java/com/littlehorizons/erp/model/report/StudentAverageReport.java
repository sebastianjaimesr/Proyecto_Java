package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Averages of a student in one academic year.
 *
 * @param subjects       one entry per subject with registrations or grades
 * @param generalAverage mean of the subject averages; subjects without grades
 *                       are excluded and counted in {@code subjectsWithoutGrades}
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
