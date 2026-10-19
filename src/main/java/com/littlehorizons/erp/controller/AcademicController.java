package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.academic.AcademicPeriod;
import com.littlehorizons.erp.model.academic.Attendance;
import com.littlehorizons.erp.model.academic.AttendanceStatus;
import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.SubjectRegistration;
import com.littlehorizons.erp.model.report.AbsenceReportRow;
import com.littlehorizons.erp.model.report.AcademicHistory;
import com.littlehorizons.erp.model.report.CourseAverageReport;
import com.littlehorizons.erp.model.report.StudentAverageReport;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordina "Gestión académica": inscripciones, notas, promedios, historial
 * y asistencia.
 *
 * <p>Patrón de todos sus métodos: llaman al servicio correspondiente dentro de
 * {@code execute(...)} (heredado de {@link BaseController}), que devuelve un
 * {@link OperationResult} con el dato y un mensaje en español; si el servicio lanza
 * una excepción, la convierte en un mensaje de error para la vista.</p>
 */
public class AcademicController extends BaseController {

    private final AcademicService academicService;
    private final AttendanceService attendanceService;

    public AcademicController(AcademicService academicService, AttendanceService attendanceService) {
        this.academicService = Objects.requireNonNull(academicService, "academicService");
        this.attendanceService = Objects.requireNonNull(attendanceService, "attendanceService");
    }

    public OperationResult<SubjectRegistration> register(int studentId, int subjectId, int academicYear,
                                                         AcademicPeriod period, String section, LocalDate date) {
        return execute(() -> academicService.register(studentId, subjectId, academicYear, period, section, date),
                registration -> "Inscripción " + registration.id() + " registrada en " + registration.subjectName()
                        + ".");
    }

    public OperationResult<List<SubjectRegistration>> registerAllSubjects(int studentId, int academicYear,
                                                                          AcademicPeriod period, String section,
                                                                          LocalDate date) {
        return execute(() -> academicService.registerAllSubjects(studentId, academicYear, period, section, date),
                list -> list.isEmpty() ? "El estudiante ya estaba inscrito en todas las materias del grado."
                        : list.size() + " inscripción(es) registrada(s).");
    }

    public OperationResult<Boolean> withdrawRegistration(int studentId, int subjectId, int academicYear,
                                                         AcademicPeriod period) {
        return execute(() -> {
            academicService.withdrawRegistration(studentId, subjectId, academicYear, period);
            return Boolean.TRUE;
        }, "Inscripción retirada. Las notas registradas se conservan.");
    }

    public OperationResult<List<SubjectRegistration>> findRegistrations(int studentId, int academicYear) {
        return execute(() -> academicService.findRegistrations(studentId, academicYear),
                list -> list.size() + " inscripción(es).");
    }

    public OperationResult<List<Student>> findStudentsInSubject(int subjectId, int academicYear, String section) {
        return execute(() -> academicService.findStudentsInSubject(subjectId, academicYear, section),
                list -> list.size() + " estudiante(s) inscrito(s).");
    }

    public OperationResult<AcademicService.GradeResult> recordGrade(int studentId, int subjectId, int teacherId,
                                                                   int academicYear, GradingPeriod period,
                                                                   BigDecimal value, String notes,
                                                                   boolean allowCorrection) {
        return execute(() -> academicService.recordGrade(studentId, subjectId, teacherId, academicYear, period,
                value, notes, allowCorrection), result -> result.corrected()
                ? "Nota corregida de " + Formats.grade(result.previousValue()) + " a "
                        + Formats.grade(result.gradeRecord().value()) + "."
                : "Nota " + Formats.grade(result.gradeRecord().value()) + " registrada.");
    }

    public OperationResult<AcademicService.GradeResult> computeFinalGrade(int studentId, int subjectId,
                                                                         int academicYear, int teacherId) {
        return execute(() -> academicService.computeFinalGrade(studentId, subjectId, academicYear, teacherId),
                result -> "Nota final calculada: " + Formats.grade(result.gradeRecord().value()) + " ("
                        + result.gradeRecord().notes() + ").");
    }

    public OperationResult<List<GradeRecord>> findGradesByStudent(int studentId, Integer academicYear) {
        return execute(() -> academicService.findGradesByStudent(studentId, academicYear),
                list -> list.size() + " nota(s).");
    }

    public OperationResult<List<GradeRecord>> findGradesBySubject(int subjectId, int academicYear, String section,
                                                                  GradingPeriod period) {
        return execute(() -> academicService.findGradesBySubject(subjectId, academicYear, section, period),
                list -> list.size() + " nota(s).");
    }

    public OperationResult<StudentAverageReport> studentAverages(int studentId, int academicYear) {
        return execute(() -> academicService.studentAverages(studentId, academicYear), "Promedios calculados.");
    }

    public OperationResult<CourseAverageReport> courseAverage(int subjectId, int academicYear, String section) {
        return execute(() -> academicService.courseAverage(subjectId, academicYear, section),
                "Promedio del curso calculado.");
    }

    public OperationResult<AcademicHistory> history(int studentId) {
        return execute(() -> academicService.history(studentId), "Historial académico cargado.");
    }

    // ------------------------------------------------------------- asistencia

    public OperationResult<Boolean> recordAttendance(int studentId, int subjectId, LocalDate date,
                                                     AttendanceStatus status, String notes, int teacherId,
                                                     boolean allowCorrection) {
        return execute(() -> attendanceService.record(studentId, subjectId, date, status, notes, teacherId,
                allowCorrection), corrected -> corrected ? "Asistencia corregida." : "Asistencia registrada.");
    }

    public OperationResult<Integer> recordGroupAttendance(int subjectId, LocalDate date, int teacherId,
                                                          Map<Integer, AttendanceStatus> statuses,
                                                          boolean allowCorrection) {
        return execute(() -> attendanceService.recordForGroup(subjectId, date, teacherId, statuses, allowCorrection),
                corrected -> statuses.size() + " registro(s) de asistencia guardados"
                        + (corrected > 0 ? " (" + corrected + " corregido(s))." : "."));
    }

    public OperationResult<List<Attendance>> attendanceHistory(int studentId, LocalDate from, LocalDate to) {
        return execute(() -> attendanceService.history(studentId, from, to), list -> list.size() + " registro(s).");
    }

    public OperationResult<List<AbsenceReportRow>> weeklyAbsenceReport(LocalDate anyDay) {
        AttendanceService.Week week = AttendanceService.Week.containing(anyDay);
        return execute(() -> attendanceService.weeklyAbsenceReport(anyDay),
                list -> list.size() + " estudiante(s) con más de " + AttendanceService.MAX_WEEKLY_ABSENCES
                        + " ausencias entre el " + week.monday() + " y el " + week.sunday() + ".");
    }

    public OperationResult<List<AbsenceReportRow>> absenceSummary(LocalDate from, LocalDate to) {
        return execute(() -> attendanceService.absenceSummary(from, to), list -> list.size() + " estudiante(s).");
    }
}
