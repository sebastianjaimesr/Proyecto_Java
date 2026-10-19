package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.report.EventTypeParticipation;
import com.littlehorizons.erp.model.report.GradeEnrollmentCount;
import com.littlehorizons.erp.model.report.SchoolStatistics;
import com.littlehorizons.erp.service.ReportService;

import java.util.List;
import java.util.Objects;

/**
 * Coordina los reportes agregados de "Reportes y estadísticas". Los reportes
 * que ya existen en otros módulos se usan a través de sus propios
 * controladores desde la vista de reportes.
 */
public class ReportController extends BaseController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
    }

    public OperationResult<List<GradeEnrollmentCount>> enrollmentsByGrade(int academicYear) {
        return execute(() -> reportService.enrollmentsByGrade(academicYear), list -> "Matrículas de " + academicYear
                + ": " + list.stream().mapToInt(GradeEnrollmentCount::active).sum() + " activas.");
    }

    public OperationResult<SchoolStatistics> statistics(int academicYear) {
        return execute(() -> reportService.statistics(academicYear), "Estadísticas calculadas.");
    }

    public OperationResult<List<EventTypeParticipation>> eventParticipation(int year) {
        return execute(() -> reportService.eventParticipationByType(year), list -> list.size() + " tipo(s) de evento.");
    }

    public OperationResult<List<ReportService.StudentRanking>> ranking(int academicYear, int limit) {
        return execute(() -> reportService.ranking(academicYear, limit), list -> list.size() + " estudiante(s).");
    }

    public OperationResult<ReportService.PaymentSummary> paymentSummary(int academicYear) {
        return execute(() -> reportService.paymentSummary(academicYear),
                summary -> summary.count() + " pago(s) registrados en " + academicYear + ".");
    }

    public OperationResult<List<ReportService.ClubOccupancy>> clubOccupancy() {
        return execute(reportService::clubOccupancy, list -> list.size() + " club(es).");
    }
}
