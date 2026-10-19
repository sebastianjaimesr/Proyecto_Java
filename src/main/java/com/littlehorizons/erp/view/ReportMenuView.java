package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.report.SchoolStatistics;
import com.littlehorizons.erp.model.resources.ResourceStatus;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pantallas de "7. Reportes y estadísticas". Todas las opciones son de solo lectura.
 */
public class ReportMenuView {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final int RANKING_SIZE = 10;

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final ReportController reports;
    private final AcademicController academic;
    private final ExtracurricularController extracurricular;
    private final FinanceController finance;
    private final ResourceController resources;
    private final StudentMenuView studentView;
    private final TeacherMenuView teacherView;
    private final AcademicMenuView academicView;
    private final ExtracurricularMenuView extracurricularView;
    private final FinanceMenuView financeView;
    private final ResourceMenuView resourceView;

    public ReportMenuView(ViewSupport support, ReportController reports, AcademicController academic,
                          ExtracurricularController extracurricular, FinanceController finance,
                          ResourceController resources, StudentMenuView studentView, TeacherMenuView teacherView,
                          AcademicMenuView academicView, ExtracurricularMenuView extracurricularView,
                          FinanceMenuView financeView, ResourceMenuView resourceView) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.reports = Objects.requireNonNull(reports, "reports");
        this.academic = Objects.requireNonNull(academic, "academic");
        this.extracurricular = Objects.requireNonNull(extracurricular, "extracurricular");
        this.finance = Objects.requireNonNull(finance, "finance");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.studentView = Objects.requireNonNull(studentView, "studentView");
        this.teacherView = Objects.requireNonNull(teacherView, "teacherView");
        this.academicView = Objects.requireNonNull(academicView, "academicView");
        this.extracurricularView = Objects.requireNonNull(extracurricularView, "extracurricularView");
        this.financeView = Objects.requireNonNull(financeView, "financeView");
        this.resourceView = Objects.requireNonNull(resourceView, "resourceView");
    }

    /** Muestra el submenú de "Reportes y estadísticas". */
    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Estudiantes matriculados por grado", this::enrollmentsByGrade),
                        new Menu.Item("Promedios de un estudiante", this::studentAverages),
                        new Menu.Item("Promedio del curso por materia", this::courseAverage),
                        new Menu.Item("Mejores promedios generales del año", this::ranking),
                        new Menu.Item("Historial académico de un estudiante", this::history),
                        new Menu.Item("Inasistencias por estudiante en un período", this::absences),
                        new Menu.Item("Participación en clubes y eventos", this::participation),
                        new Menu.Item("Resultados de competencias", this::competitionResults),
                        new Menu.Item("Pagos por estudiante o acudiente", this::paymentsByPerson),
                        new Menu.Item("Resumen de pagos del año", this::paymentSummary),
                        new Menu.Item("Obligaciones vencidas", this::overdue),
                        new Menu.Item("Recursos disponibles y prestados", this::resources),
                        new Menu.Item("Préstamos vencidos", this::overdueLoans),
                        new Menu.Item("Estadísticas generales del colegio", this::statistics)),
                "REPORTES Y ESTADÍSTICAS").run();
    }

    /** Reporte de estudiantes matriculados por grado. */
    private void enrollmentsByGrade() {
        console.printTitle("Estudiantes matriculados por grado");
        int year = support.readYear("Año académico");
        support.show(reports.enrollmentsByGrade(year)).ifPresent(rows -> {
            console.printTable(List.of("Grado", "Activas", "Completadas", "Retiradas", "Total"), rows.stream()
                    .map(row -> List.of(row.gradeName(), String.valueOf(row.active()), String.valueOf(row.completed()),
                            String.valueOf(row.withdrawn()), String.valueOf(row.total()))).collect(Collectors.toList()));
            console.println("Total de matrículas del año: " + rows.stream().mapToInt(row -> row.total()).sum());
        });
        console.pause();
    }

    /** Reporte de promedios de un estudiante. */
    private void studentAverages() {
        console.printTitle("Promedios de un estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(academic.studentAverages(student.get().id(), year)).ifPresent(academicView::printStudentAverages);
        console.pause();
    }

    /** Reporte del promedio del curso en una materia. */
    private void courseAverage() {
        console.printTitle("Promedio del curso por materia");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        String section = support.readSection("Sección (Enter para todo el grado)");
        support.show(academic.courseAverage(subject.get().id(), year, section)).ifPresent(academicView::printCourseAverage);
        console.pause();
    }

    /** Reporte de los mejores promedios generales del año. */
    private void ranking() {
        console.printTitle("Mejores promedios generales");
        int year = support.readYear("Año académico");
        support.show(reports.ranking(year, RANKING_SIZE)).ifPresent(rows -> console.printTable(
                List.of("Puesto", "Estudiante", "Promedio general", "Materias con nota"), rows.stream()
                        .map(row -> List.of(String.valueOf(row.position()), row.studentName(),
                                Formats.grade(row.average()), String.valueOf(row.subjects())))
                        .collect(Collectors.toList())));
        console.pause();
    }

    /** Reporte del historial académico de un estudiante. */
    private void history() {
        console.printTitle("Historial académico");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(academic.history(student.get().id())).ifPresent(academicView::printHistory);
        console.pause();
    }

    /** Reporte de inasistencias por estudiante en un rango de fechas. */
    private void absences() {
        console.printTitle("Inasistencias por estudiante");
        console.printInfo("Indique el período a analizar (por ejemplo las fechas de un período académico).");
        LocalDate from = support.readDate("Desde");
        LocalDate to = support.readDateOrToday("Hasta");
        support.show(academic.absenceSummary(from, to)).ifPresent(academicView::printAbsenceRows);
        console.pause();
    }

    /** Reporte de ocupación de los clubes y participación en eventos por tipo. */
    private void participation() {
        console.printTitle("Participación en clubes");
        support.show(reports.clubOccupancy()).ifPresent(rows -> console.printTable(
                List.of("Club", "Miembros activos", "Cupo", "Ocupación", "Activo"), rows.stream()
                        .map(row -> List.of(row.clubName(), String.valueOf(row.activeMembers()),
                                String.valueOf(row.capacity()), row.occupancyPercent().toPlainString() + " %",
                                Formats.yesNo(row.active()))).collect(Collectors.toList())));
        console.printTitle("Participación en eventos");
        int year = support.readYear("Año de los eventos");
        support.show(reports.eventParticipation(year)).ifPresent(rows -> console.printTable(
                List.of("Tipo de evento", "Eventos", "Participaciones", "Estudiantes distintos"), rows.stream()
                        .map(row -> List.of(row.eventType(), String.valueOf(row.events()),
                                String.valueOf(row.participations()), String.valueOf(row.distinctStudents())))
                        .collect(Collectors.toList())));
        console.pause();
    }

    /** Reporte de resultados de competencias de un año. */
    private void competitionResults() {
        console.printTitle("Resultados de competencias");
        int year = support.readYear("Año");
        support.show(extracurricular.findResults(year)).ifPresent(extracurricularView::printParticipants);
        console.pause();
    }

    /** Reporte de pagos de un estudiante o de un acudiente. */
    private void paymentsByPerson() {
        console.printTitle("Pagos por estudiante o acudiente");
        if (console.confirm("¿Consultar por estudiante? (N = por acudiente)")) {
            Optional<Student> student = studentView.pickStudent();
            if (student.isPresent()) {
                support.show(finance.historyByStudent(student.get().id(), null)).ifPresent(financeView::printPayments);
            }
        } else {
            Optional<Guardian> guardian = studentView.pickGuardian();
            if (guardian.isPresent()) {
                support.show(finance.historyByGuardian(guardian.get().id())).ifPresent(financeView::printPayments);
            }
        }
        console.pause();
    }

    /** Reporte de recaudo del año: total y totales por concepto, método de pago y mes. */
    private void paymentSummary() {
        console.printTitle("Resumen de pagos del año");
        int year = support.readYear("Año académico");
        support.show(reports.paymentSummary(year)).ifPresent(summary -> {
            console.println("Total recaudado: " + Formats.money(summary.total()) + " en " + summary.count() + " pago(s).");
            printAmounts("Concepto", summary.byConcept());
            printAmounts("Método de pago", summary.byMethod());
            printAmounts("Mes", summary.byMonth().entrySet().stream().collect(Collectors.toMap(
                    entry -> String.format("%02d %s", entry.getKey(),
                            Month.of(entry.getKey()).getDisplayName(TextStyle.FULL, SPANISH)),
                    Map.Entry::getValue, (a, b) -> a, java.util.TreeMap::new)));
        });
        console.pause();
    }

    /** Imprime una tabla de etiqueta y valor en pesos. */
    private void printAmounts(String label, Map<String, java.math.BigDecimal> amounts) {
        console.printTable(List.of(label, "Valor"), amounts.entrySet().stream()
                .map(entry -> List.of(entry.getKey(), Formats.money(entry.getValue()))).collect(Collectors.toList()));
    }

    /** Reporte de obligaciones vencidas (morosidad). */
    private void overdue() {
        console.printTitle("Obligaciones vencidas");
        int year = support.readYear("Año académico");
        support.show(finance.overdue(year)).ifPresent(financeView::printOverdue);
        console.pause();
    }

    /** Reporte de disponibilidad de recursos y lista de recursos prestados. */
    private void resources() {
        console.printTitle("Recursos disponibles y prestados");
        support.quiet(resources.availability()).ifPresent(resourceView::printAvailability);
        console.println("Recursos prestados:");
        support.show(resources.searchResources(null, null, ResourceStatus.LOANED)).ifPresent(list -> console.printTable(
                List.of("Código", "Recurso", "Categoría", "Ubicación"), list.stream()
                        .map(resource -> List.of(resource.code(), resource.name(), resource.categoryName(),
                                Formats.text(resource.location()))).collect(Collectors.toList())));
        console.pause();
    }

    /** Reporte de préstamos vencidos. */
    private void overdueLoans() {
        console.printTitle("Préstamos vencidos");
        support.show(resources.findOverdue()).ifPresent(resourceView::printLoans);
        console.pause();
    }

    /** Reporte de estadísticas generales del colegio. */
    private void statistics() {
        console.printTitle("Estadísticas generales");
        int year = support.readYear("Año académico");
        support.show(reports.statistics(year)).ifPresent(this::printStatistics);
        console.pause();
    }

    /** Imprime las estadísticas generales como tabla de indicador y valor. */
    private void printStatistics(SchoolStatistics stats) {
        console.printTable(List.of("Indicador", "Valor"), List.of(
                List.of("Estudiantes activos", String.valueOf(stats.activeStudents())),
                List.of("Matrículas activas " + stats.academicYear(), String.valueOf(stats.activeEnrollments())),
                List.of("Profesores activos", String.valueOf(stats.activeTeachers())),
                List.of("Acudientes registrados", String.valueOf(stats.guardians())),
                List.of("Clubes activos / miembros activos", stats.activeClubs() + " / " + stats.activeClubMembers()),
                List.of("Eventos / participaciones del año", stats.events() + " / " + stats.eventParticipations()),
                List.of("Recursos / prestados", stats.resources() + " / " + stats.loanedResources()),
                List.of("Préstamos sin devolver", String.valueOf(stats.openLoans())),
                List.of("Pagos del año", stats.paymentsCount() + " por " + Formats.money(stats.paymentsTotal())),
                List.of("Promedio de notas I-IV del año", Formats.grade(stats.averageGrade())
                        + " (" + stats.gradeRecords() + " registros)"),
                List.of("Ausencias registradas en el año", String.valueOf(stats.absences()))));
    }
}
