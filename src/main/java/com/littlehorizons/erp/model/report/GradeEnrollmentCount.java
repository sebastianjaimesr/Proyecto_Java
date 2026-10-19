package com.littlehorizons.erp.model.report;

/**
 * Matrículas de un grado en un año, por estado.
 */
public record GradeEnrollmentCount(String gradeName, int level, int active, int completed, int withdrawn) {

    public int total() {
        return active + completed + withdrawn;
    }
}
