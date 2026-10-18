package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Academic history of a student since admission: one entry per year with
 * grades or enrollment.
 */
public record AcademicHistory(int studentId, String studentName, List<YearSummary> years) {

    public AcademicHistory {
        years = List.copyOf(years);
    }

    /** Grades of one year. {@code gradeName} and {@code enrollmentStatus} may be null without enrollment. */
    public record YearSummary(
            int academicYear,
            String gradeName,
            String enrollmentStatus,
            List<SubjectAverage> subjects,
            BigDecimal generalAverage) {

        public YearSummary {
            subjects = List.copyOf(subjects);
        }
    }
}
