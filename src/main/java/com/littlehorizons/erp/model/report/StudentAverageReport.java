package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.academic.GradingPeriod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Averages of a student in one academic year.
 *
 * @param subjects       one entry per subject with registrations or grades
 * @param periodAverages general average of the student in each period (I to IV)
 *                       that has grades: mean of the subject grades of that period
 * @param generalAverage mean of the subject averages; subjects without grades
 *                       are excluded and counted in {@code subjectsWithoutGrades}
 */
public record StudentAverageReport(
        int studentId,
        String studentName,
        int academicYear,
        String gradeName,
        List<SubjectAverage> subjects,
        Map<GradingPeriod, BigDecimal> periodAverages,
        BigDecimal generalAverage,
        long subjectsWithoutGrades) {

    public StudentAverageReport {
        subjects = List.copyOf(subjects);
        periodAverages = Map.copyOf(periodAverages);
    }
}
