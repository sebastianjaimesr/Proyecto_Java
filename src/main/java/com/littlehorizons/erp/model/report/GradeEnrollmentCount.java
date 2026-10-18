package com.littlehorizons.erp.model.report;

/**
 * Enrollments of one grade in a year, by status.
 */
public record GradeEnrollmentCount(String gradeName, int level, int active, int completed, int withdrawn) {

    public int total() {
        return active + completed + withdrawn;
    }
}
