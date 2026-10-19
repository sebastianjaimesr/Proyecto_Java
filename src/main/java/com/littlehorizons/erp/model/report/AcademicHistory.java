package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Historial académico de un estudiante desde su ingreso: una entrada por año con
 * notas o matrícula.
 */
public record AcademicHistory(int studentId, String studentName, List<YearSummary> years) {

    public AcademicHistory {
        years = List.copyOf(years);
    }

    /** Notas de un año. {@code gradeName} y {@code enrollmentStatus} pueden ser null si no hay matrícula. */
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
