package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.academic.GradingPeriod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Notas de un estudiante en una materia y año, y el promedio resultante.
 *
 * @param grades       nota por período (I a IV y Final, si existe)
 * @param average      la nota Final si existe; si no, la media de los
 *                     períodos con nota; {@code null} si no hay ninguna
 * @param finalGrade   si {@code average} es la nota Final guardada
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

    /** Cantidad de períodos regulares (I a IV) con nota. */
    public long gradedPeriods() {
        return grades.keySet().stream().filter(GradingPeriod::isEditable).count();
    }
}
