package com.littlehorizons.erp.model.report;

/**
 * Attendance counts of one student in a date range. Only "Ausente" counts as
 * an unexcused absence; late arrivals and medical excuses are shown apart.
 */
public record AbsenceReportRow(
        int studentId,
        String documentNumber,
        String studentName,
        String gradeName,
        int absences,
        int lateArrivals,
        int medicalExcuses,
        int totalRecords) {
}
