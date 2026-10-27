package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.academic.AcademicPeriod;
import com.littlehorizons.erp.model.academic.Attendance;
import com.littlehorizons.erp.model.academic.AttendanceStatus;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.SubjectRegistration;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.report.AbsenceReportRow;
import com.littlehorizons.erp.model.report.AcademicHistory;
import com.littlehorizons.erp.model.report.CourseAverageReport;
import com.littlehorizons.erp.model.report.StudentAverageReport;
import com.littlehorizons.erp.model.report.SubjectAverage;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Screens of "2. Gestión académica": subjects, registrations, grades,
 * averages, academic history and attendance.
 */
public class AcademicMenuView {

    private static final List<GradingPeriod> EDITABLE_PERIODS = Arrays.stream(GradingPeriod.values())
            .filter(GradingPeriod::isEditable).collect(Collectors.toList());

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final AcademicController controller;
    private final TeacherController teacherController;
    private final StudentMenuView studentView;
    private final TeacherMenuView teacherView;

    public AcademicMenuView(ViewSupport support, AcademicController controller, TeacherController teacherController,
                            StudentMenuView studentView, TeacherMenuView teacherView) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.controller = Objects.requireNonNull(controller, "controller");
        this.teacherController = Objects.requireNonNull(teacherController, "teacherController");
        this.studentView = Objects.requireNonNull(studentView, "studentView");
        this.teacherView = Objects.requireNonNull(teacherView, "teacherView");
    }

    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Materias", this::showSubjectsMenu),
                        new Menu.Item("Inscripciones en materias", this::showRegistrationsMenu),
                        new Menu.Item("Calificaciones y promedios", this::showGradesMenu),
                        new Menu.Item("Asistencia", this::showAttendanceMenu)),
                "GESTIÓN ACADÉMICA").run();
    }

    private void showSubjectsMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Registrar materia", this::registerSubject),
                        new Menu.Item("Actualizar, activar o desactivar una materia", this::updateSubject),
                        new Menu.Item("Consultar materias por grado", this::listSubjects)),
                "MATERIAS").run();
    }

    private void showRegistrationsMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Inscribir estudiante en una materia", this::registerOne),
                        new Menu.Item("Inscribir estudiante en todas las materias de su grado", this::registerAll),
                        new Menu.Item("Retirar una inscripción", this::withdrawRegistration),
                        new Menu.Item("Consultar inscripciones de un estudiante", this::listRegistrations)),
                "INSCRIPCIONES").run();
    }

    private void showGradesMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Registrar o corregir nota (períodos I a IV)", this::recordGrade),
                        new Menu.Item("Calcular nota final de una materia", this::computeFinal),
                        new Menu.Item("Consultar notas de un estudiante", this::gradesByStudent),
                        new Menu.Item("Consultar notas por materia, grupo y período", this::gradesBySubject),
                        new Menu.Item("Promedios de un estudiante (por materia y general)", this::studentAverages),
                        new Menu.Item("Promedio del curso por materia", this::courseAverage),
                        new Menu.Item("Historial académico desde el ingreso", this::history),
                        new Menu.Item("Exportar boletín de un estudiante (TXT)", this::exportReportCard),
                        new Menu.Item("Exportar lista de clase con promedios (TXT)", this::exportClassList),
                        new Menu.Item("Importar notas desde archivo (todo o nada)", this::importGrades)),
                "CALIFICACIONES Y PROMEDIOS").run();
    }

    private void showAttendanceMenu() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Tomar asistencia de un grupo", this::recordGroupAttendance),
                        new Menu.Item("Registrar o corregir asistencia de un estudiante", this::recordAttendance),
                        new Menu.Item("Consultar asistencia histórica de un estudiante", this::attendanceHistory),
                        new Menu.Item("Reporte semanal: más de 3 ausencias", this::weeklyReport),
                        new Menu.Item("Generar planilla de asistencia (TXT)", this::exportAttendanceSheet),
                        new Menu.Item("Registrar asistencia desde planilla", this::recordFromSheet)),
                "ASISTENCIA").run();
    }

    // -------------------------------------------------------------- subjects

    private void registerSubject() {
        console.printTitle("Registrar materia");
        Optional<List<Grade>> grades = support.quiet(teacherController.findGrades());
        if (grades.isEmpty()) {
            console.pause();
            return;
        }
        console.println("Grado:");
        Grade grade = console.readChoice("Opción: ", grades.get(), Grade::name, "grado");
        String code = console.readCode("Código único (ej: MAT-5): ", "código", 20);
        String name = console.readRequiredText("Nombre: ", "nombre", 100);
        String description = support.readOptional("Descripción", null,
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        int hours = console.readInt("Horas semanales (1-20): ", "horas semanales", 1, 20);
        support.show(teacherController.registerSubject(new Subject(null, code, name, description, grade.id(),
                grade.name(), hours, true)));
        console.pause();
    }

    private void updateSubject() {
        console.printTitle("Actualizar materia");
        Optional<Subject> selected = teacherView.pickSubject(false);
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        Subject current = selected.get();
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        String code = console.readValid("Código [" + current.code() + "]: ", raw -> raw.isBlank()
                ? current.code() : InputValidator.validateCode(raw, "código", 20));
        String name = support.readTextOrKeep("Nombre", current.name(), true);
        String description = support.readOptional("Descripción", current.description(),
                raw -> InputValidator.requireText(raw, "descripción", 2000));
        int hours = console.readValid("Horas semanales [" + current.weeklyHours() + "]: ", raw -> raw.isBlank()
                ? current.weeklyHours() : InputValidator.parseInt(raw, "horas semanales", 1, 20));
        boolean active = console.confirm("¿La materia está activa?");
        support.show(teacherController.updateSubject(new Subject(current.id(), code, name, description,
                current.gradeId(), current.gradeName(), hours, active)));
        console.pause();
    }

    private void listSubjects() {
        console.printTitle("Materias por grado");
        Optional<List<Grade>> grades = support.quiet(teacherController.findGrades());
        if (grades.isEmpty()) {
            console.pause();
            return;
        }
        console.println("Grado:");
        Grade grade = console.readChoice("Opción: ", grades.get(), Grade::name, "grado");
        support.show(teacherController.findSubjectsByGrade(grade.id(), false)).ifPresent(subjects ->
                console.printTable(List.of("Id", "Código", "Materia", "Horas/semana", "Activa"), subjects.stream()
                        .map(subject -> List.of(String.valueOf(subject.id()), subject.code(), subject.name(),
                                String.valueOf(subject.weeklyHours()), Formats.yesNo(subject.active())))
                        .collect(Collectors.toList())));
        console.pause();
    }

    // --------------------------------------------------------- registrations

    private void registerOne() {
        console.printTitle("Inscribir en una materia");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Subject> subject = teacherView.pickSubject(true);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        AcademicPeriod period = support.readEnum("Período", AcademicPeriod.class);
        String section = support.readSection("Sección o grupo");
        LocalDate date = support.readDateOrToday("Fecha de inscripción");
        support.show(controller.register(student.get().id(), subject.get().id(), year, period, section, date));
        console.pause();
    }

    private void registerAll() {
        console.printTitle("Inscribir en todas las materias del grado");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        AcademicPeriod period = support.readEnum("Período", AcademicPeriod.class);
        String section = support.readSection("Sección o grupo");
        LocalDate date = support.readDateOrToday("Fecha de inscripción");
        support.show(controller.registerAllSubjects(student.get().id(), year, period, section, date))
                .ifPresent(this::printRegistrations);
        console.pause();
    }

    private void withdrawRegistration() {
        console.printTitle("Retirar inscripción");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        Optional<List<SubjectRegistration>> registrations =
                support.quiet(controller.findRegistrations(student.get().id(), year));
        if (registrations.isEmpty() || registrations.get().isEmpty()) {
            console.printWarning("El estudiante no tiene inscripciones en " + year + ".");
            console.pause();
            return;
        }
        SubjectRegistration registration = console.readChoice("Inscripción: ", registrations.get(),
                item -> item.subjectName() + " - período " + item.period().dbValue() + " (" + item.status().dbValue() + ")",
                "inscripción");
        if (console.confirm("¿Retirar la inscripción? Las notas ya registradas se conservan.")) {
            support.show(controller.withdrawRegistration(student.get().id(), registration.subjectId(), year,
                    registration.period()));
        }
        console.pause();
    }

    private void listRegistrations() {
        console.printTitle("Inscripciones");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(controller.findRegistrations(student.get().id(), year)).ifPresent(this::printRegistrations);
        console.pause();
    }

    private void printRegistrations(List<SubjectRegistration> registrations) {
        console.printTable(List.of("Id", "Materia", "Período", "Sección", "Fecha", "Estado"), registrations.stream()
                .map(item -> List.of(String.valueOf(item.id()), item.subjectName(), item.period().dbValue(),
                        Formats.text(item.section()), Formats.date(item.registrationDate()), item.status().dbValue()))
                .collect(Collectors.toList()));
    }

    // ---------------------------------------------------------------- grades

    private void recordGrade() {
        console.printTitle("Registrar nota");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        console.println("Profesor que registra la nota:");
        Optional<Teacher> teacher = teacherView.pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        GradingPeriod period = support.readEnum("Período", EDITABLE_PERIODS);
        BigDecimal value = console.readGrade("Nota (0.0 a 5.0, un decimal): ");
        String notes = support.readOptional("Observaciones", null,
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        OperationResult<AcademicService.GradeResult> result = controller.recordGrade(student.get().id(),
                subject.get().id(), teacher.get().id(), year, period, value, notes, false);
        if (result.confirmationRequired()) {
            console.printWarning(result.message());
            if (console.confirm("¿Corregir la nota existente?")) {
                result = controller.recordGrade(student.get().id(), subject.get().id(), teacher.get().id(), year,
                        period, value, notes, true);
            } else {
                support.notFound();
                console.pause();
                return;
            }
        }
        support.show(result);
        console.pause();
    }

    private void computeFinal() {
        console.printTitle("Calcular nota final");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        console.println("Profesor responsable:");
        Optional<Teacher> teacher = teacherView.pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        console.printInfo("La nota final es el promedio simple de los períodos I a IV que tengan nota.");
        support.show(controller.computeFinalGrade(student.get().id(), subject.get().id(), year, teacher.get().id()));
        console.pause();
    }

    private void gradesByStudent() {
        console.printTitle("Notas de un estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        Optional<GradingPeriod> period = readOptionalPeriod();
        support.show(controller.findGradesByStudent(student.get().id(), year)).ifPresent(records ->
                printGrades(records.stream().filter(item -> period.isEmpty() || item.period() == period.get())
                        .collect(Collectors.toList()), false));
        console.pause();
    }

    private void gradesBySubject() {
        console.printTitle("Notas por materia, grupo y período");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        String section = support.readSection("Sección o grupo (Enter para todos)");
        Optional<GradingPeriod> period = readOptionalPeriod();
        support.show(controller.findGradesBySubject(subject.get().id(), year, section, period.orElse(null)))
                .ifPresent(records -> printGrades(records, true));
        console.pause();
    }

    private Optional<GradingPeriod> readOptionalPeriod() {
        if (!console.confirm("¿Filtrar por un período?")) {
            return Optional.empty();
        }
        return Optional.of(support.readEnum("Período", GradingPeriod.class));
    }

    private void printGrades(List<GradeRecord> records, boolean showStudent) {
        console.printTable(showStudent
                        ? List.of("Estudiante", "Período", "Nota", "Profesor", "Fecha")
                        : List.of("Año", "Materia", "Período", "Nota", "Profesor", "Fecha"),
                records.stream().map(item -> showStudent
                        ? List.of(item.studentName(), item.period().dbValue(), Formats.grade(item.value()),
                        item.teacherName(), Formats.date(item.recordDate()))
                        : List.of(String.valueOf(item.academicYear()), item.subjectName(), item.period().dbValue(),
                        Formats.grade(item.value()), item.teacherName(), Formats.date(item.recordDate())))
                        .collect(Collectors.toList()));
    }

    private void studentAverages() {
        console.printTitle("Promedios del estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(controller.studentAverages(student.get().id(), year)).ifPresent(this::printStudentAverages);
        console.pause();
    }

    void printStudentAverages(StudentAverageReport report) {
        console.println(report.studentName() + " · " + report.academicYear()
                + (report.gradeName() == null ? "" : " · " + report.gradeName()));
        printSubjectAverages(report.subjects());
        if (!report.periodAverages().isEmpty()) {
            console.println("Promedio general por período: " + EDITABLE_PERIODS.stream()
                    .filter(report.periodAverages()::containsKey)
                    .map(period -> period.dbValue() + " = " + Formats.grade(report.periodAverages().get(period)))
                    .collect(Collectors.joining(" · ")));
        }
        console.println("Promedio general del año: " + Formats.grade(report.generalAverage())
                + (report.subjectsWithoutGrades() > 0
                ? " (" + report.subjectsWithoutGrades() + " materia(s) sin calificaciones no se incluyen)" : ""));
    }

    private void printSubjectAverages(List<SubjectAverage> subjects) {
        console.printTable(List.of("Materia", "I", "II", "III", "IV", "Final", "Promedio"), subjects.stream()
                .map(subject -> List.of(subject.subjectName(),
                        Formats.grade(subject.grades().get(GradingPeriod.I)),
                        Formats.grade(subject.grades().get(GradingPeriod.II)),
                        Formats.grade(subject.grades().get(GradingPeriod.III)),
                        Formats.grade(subject.grades().get(GradingPeriod.IV)),
                        Formats.grade(subject.grades().get(GradingPeriod.FINAL)),
                        subject.hasGrades() ? Formats.grade(subject.average()) : "Sin calificaciones"))
                .collect(Collectors.toList()));
    }

    private void courseAverage() {
        console.printTitle("Promedio del curso por materia");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        String section = support.readSection("Sección o grupo (Enter para todo el grado)");
        support.show(controller.courseAverage(subject.get().id(), year, section)).ifPresent(this::printCourseAverage);
        console.pause();
    }

    void printCourseAverage(CourseAverageReport report) {
        console.println(report.subjectName() + " · " + report.gradeName() + " · " + report.academicYear()
                + (report.section() == null ? "" : " · sección " + report.section()));
        console.printTable(List.of("Estudiante", "Promedio", "Origen"), report.students().stream()
                .map(student -> List.of(student.studentName(), Formats.grade(student.average()),
                        student.finalGrade() ? "Nota final" : "Promedio de períodos"))
                .collect(Collectors.toList()));
        console.println("Promedio del curso: " + Formats.grade(report.courseAverage())
                + " · Mayor: " + Formats.grade(report.highest()) + " · Menor: " + Formats.grade(report.lowest())
                + " · Estudiantes con notas: " + report.students().size());
    }

    private void history() {
        console.printTitle("Historial académico");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        support.show(controller.history(student.get().id())).ifPresent(this::printHistory);
        console.pause();
    }

    void printHistory(AcademicHistory history) {
        console.println(history.studentName());
        if (history.years().isEmpty()) {
            console.printWarning("No hay matrículas ni notas registradas.");
            return;
        }
        for (AcademicHistory.YearSummary year : history.years()) {
            console.println();
            console.println("Año " + year.academicYear() + " · " + Formats.text(year.gradeName()) + " · matrícula "
                    + Formats.text(year.enrollmentStatus()) + " · promedio general " + Formats.grade(year.generalAverage()));
            if (year.subjects().isEmpty()) {
                console.println("  Sin calificaciones registradas.");
            } else {
                printSubjectAverages(year.subjects());
            }
        }
    }

    // ------------------------------------------------------------ attendance

    private void recordGroupAttendance() {
        console.printTitle("Tomar asistencia de un grupo");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        console.println("Profesor que toma la asistencia:");
        Optional<Teacher> teacher = teacherView.pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate date = support.readDateOrToday("Fecha");
        String section = support.readSection("Sección o grupo (Enter para todos)");
        Optional<List<Student>> students = support.quiet(controller.findStudentsInSubject(subject.get().id(),
                date.getYear(), section));
        if (students.isEmpty() || students.get().isEmpty()) {
            console.printWarning("No hay estudiantes inscritos en esa materia y grupo en " + date.getYear() + ".");
            console.pause();
            return;
        }
        console.printInfo("Para cada estudiante: P = Presente, A = Ausente, T = Tardanza, E = Excusa Médica.");
        Map<Integer, AttendanceStatus> statuses = new LinkedHashMap<>();
        for (Student student : students.get()) {
            statuses.put(student.id(), console.readValid(student.name().sortableName() + " [P/A/T/E]: ",
                    AcademicMenuView::parseAttendanceLetter));
        }
        OperationResult<Integer> result = controller.recordGroupAttendance(subject.get().id(), date,
                teacher.get().id(), statuses, false);
        if (result.confirmationRequired()) {
            console.printWarning(result.message());
            if (console.confirm("¿Reemplazar la asistencia ya registrada de ese día?")) {
                result = controller.recordGroupAttendance(subject.get().id(), date, teacher.get().id(), statuses, true);
            } else {
                support.notFound();
                console.pause();
                return;
            }
        }
        support.show(result);
        console.pause();
    }

    static AttendanceStatus parseAttendanceLetter(String raw) {
        switch (raw.strip().toUpperCase(java.util.Locale.ROOT)) {
            case "P":
                return AttendanceStatus.PRESENT;
            case "A":
                return AttendanceStatus.ABSENT;
            case "T":
                return AttendanceStatus.LATE;
            case "E":
                return AttendanceStatus.MEDICAL_EXCUSE;
            default:
                throw new com.littlehorizons.erp.exception.ValidationException(
                        "Escriba P (Presente), A (Ausente), T (Tardanza) o E (Excusa Médica).");
        }
    }

    private void recordAttendance() {
        console.printTitle("Asistencia de un estudiante");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        console.println("Profesor que registra:");
        Optional<Teacher> teacher = teacherView.pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate date = support.readDateOrToday("Fecha");
        AttendanceStatus status = support.readEnum("Estado", AttendanceStatus.class);
        String notes = support.readOptional("Observaciones", null,
                raw -> InputValidator.requireText(raw, "observaciones", 1000));
        OperationResult<Boolean> result = controller.recordAttendance(student.get().id(), subject.get().id(), date,
                status, notes, teacher.get().id(), false);
        if (result.confirmationRequired()) {
            console.printWarning(result.message());
            if (console.confirm("¿Corregir el registro existente?")) {
                result = controller.recordAttendance(student.get().id(), subject.get().id(), date, status, notes,
                        teacher.get().id(), true);
            } else {
                support.notFound();
                console.pause();
                return;
            }
        }
        support.show(result);
        console.pause();
    }

    private void attendanceHistory() {
        console.printTitle("Asistencia histórica");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate from = support.readDate("Desde");
        LocalDate to = support.readDateOrToday("Hasta");
        support.show(controller.attendanceHistory(student.get().id(), from, to)).ifPresent(records -> {
            console.printTable(List.of("Fecha", "Materia", "Estado", "Observaciones"), records.stream()
                    .map((Attendance item) -> List.of(Formats.date(item.date()), item.subjectName(),
                            item.status().dbValue(), Formats.text(item.notes())))
                    .collect(Collectors.toList()));
            Map<AttendanceStatus, Long> totals = records.stream()
                    .collect(Collectors.groupingBy(Attendance::status, Collectors.counting()));
            console.println("Totales: " + Arrays.stream(AttendanceStatus.values())
                    .map(status -> status.dbValue() + " " + totals.getOrDefault(status, 0L))
                    .collect(Collectors.joining(" · ")));
        });
        console.pause();
    }

    private void weeklyReport() {
        console.printTitle("Reporte semanal de inasistencias");
        console.printInfo("Semana de lunes a domingo que contiene la fecha indicada. Solo cuenta \"Ausente\".");
        LocalDate date = support.readDateOrToday("Cualquier fecha de la semana");
        support.show(controller.weeklyReportWithHistory(date)).ifPresent(history -> {
            printAbsenceRows(history.rows());
            console.printInfo(history.added()
                    ? "Semana agregada al histórico: " + history.file()
                    : "Esta semana ya estaba en el histórico (" + history.file() + "); no se repitió.");
        });
        console.pause();
    }

    // ------------------------------------------------------ text file exercises

    private void exportReportCard() {
        console.printTitle("Exportar boletín");
        Optional<Student> student = studentView.pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(controller.exportReportCard(student.get().id(), year));
        console.pause();
    }

    private void exportClassList() {
        console.printTitle("Exportar lista de clase");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        String section = support.readSection("Sección o grupo (Enter para todo el grado)");
        support.show(controller.exportClassList(subject.get().id(), year, section));
        console.pause();
    }

    private void importGrades() {
        console.printTitle("Importar notas desde archivo");
        console.printInfo("Formato por línea: documento;periodo;nota  (ejemplo: 1012345678;I;4.5)");
        console.printInfo("Si alguna línea tiene error no se guarda ninguna nota.");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        console.println("Profesor que registra las notas:");
        Optional<Teacher> teacher = teacherView.pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        Path file = support.readPath("Archivo de notas");
        boolean replace = console.confirm("¿Reemplazar las notas que ya existan?");
        support.showImport(controller.importGrades(file, subject.get().id(), teacher.get().id(), year, replace));
        console.pause();
    }

    private void exportAttendanceSheet() {
        console.printTitle("Generar planilla de asistencia");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate date = support.readDateOrToday("Fecha de la clase");
        String section = support.readSection("Sección o grupo (Enter para todos)");
        support.show(controller.exportAttendanceSheet(subject.get().id(), section, date));
        console.pause();
    }

    private void recordFromSheet() {
        console.printTitle("Registrar asistencia desde planilla");
        Optional<Subject> subject = teacherView.pickSubject(false);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        console.println("Profesor que toma la asistencia:");
        Optional<Teacher> teacher = teacherView.pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        LocalDate date = support.readDateOrToday("Fecha de la clase");
        String section = support.readSection("Sección o grupo (Enter para todos)");
        Path file = support.readPath("Planilla diligenciada");
        OperationResult<AttendanceService.SheetResult> result = controller.recordFromSheet(file,
                subject.get().id(), section, date, teacher.get().id(), false);
        if (result.confirmationRequired()) {
            console.printWarning(result.message());
            if (!console.confirm("¿Reemplazar la asistencia ya registrada de ese día?")) {
                support.notFound();
                console.pause();
                return;
            }
            result = controller.recordFromSheet(file, subject.get().id(), section, date, teacher.get().id(), true);
        }
        support.show(result).ifPresent(sheet -> sheet.warnings().forEach(console::printWarning));
        console.pause();
    }

    void printAbsenceRows(List<AbsenceReportRow> rows) {
        console.printTable(List.of("Documento", "Estudiante", "Grado", "Ausencias", "Tardanzas", "Excusas"),
                rows.stream().map(row -> List.of(Formats.maskDocument(row.documentNumber()), row.studentName(),
                        Formats.text(row.gradeName()), String.valueOf(row.absences()),
                        String.valueOf(row.lateArrivals()), String.valueOf(row.medicalExcuses())))
                        .collect(Collectors.toList()));
    }
}
