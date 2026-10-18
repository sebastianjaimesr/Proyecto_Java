package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.academic.GradingPeriod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Grades of one student in one subject and year, and the resulting average.
 *
 * @param grades       grade per period (I to IV and Final, when present)
 * @param average      Final grade when it exists, otherwise the mean of the
 *                     periods with grades; {@code null} when there are none
 * @param finalGrade   whether {@code average} is the stored Final grade
 */
public record SubjectAverage(
        int subjectId,
        String subjectName,
        Map<GradingPeriod, BigDecimal> grades,
        BigDecimal average,
        boolean finalGrade) {

    public SubjectAverage {
        grades = Map.copyOf(grades);
    }

    public boolean hasGrades() {
        return average != null;
    }

    /** Number of regular periods (I to IV) with a grade. */
    public long gradedPeriods() {
        return grades.keySet().stream().filter(GradingPeriod::isEditable).count();
    }
}
