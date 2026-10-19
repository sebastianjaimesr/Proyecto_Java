package com.littlehorizons.erp.model.report;

/**
 * Conteos de asistencia de un estudiante en un rango de fechas. Solo "Ausente" cuenta como
 * inasistencia sin excusa; las tardanzas y excusas médicas se muestran aparte.
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
