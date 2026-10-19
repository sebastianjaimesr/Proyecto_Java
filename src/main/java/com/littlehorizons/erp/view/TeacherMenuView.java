package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Subject;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.academic.WeekDay;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pantallas de "3. Gestión de profesores": profesores, asignaciones de materias y
 * horarios semanales de clase.
 */
public class TeacherMenuView {

    private static final List<String> TEACHER_HEADERS = List.of("Id", "Documento", "Nombre", "Especialización", "Estado");

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final TeacherController controller;

    public TeacherMenuView(ViewSupport support, TeacherController controller) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Registrar profesor", this::registerTeacher),
                        new Menu.Item("Consultar profesores", this::listTeachers),
                        new Menu.Item("Actualizar datos o estado de un profesor", this::updateTeacher),
                        new Menu.Item("Asignar materia y grupo a un profesor", this::assign),
                        new Menu.Item("Consultar asignaciones de un profesor", this::listAssignments),
                        new Menu.Item("Registrar día y hora de clase", this::addSchedule),
                        new Menu.Item("Consultar horario semanal de un profesor", this::showTimetable),
                        new Menu.Item("Eliminar un bloque de horario", this::removeSchedule)),
                "GESTIÓN DE PROFESORES").run();
    }

    Optional<Teacher> pickTeacher() {
        return support.pick("profesor", controller::searchTeachers, Teacher::id, TEACHER_HEADERS,
                teacher -> List.of(String.valueOf(teacher.id()), Formats.maskDocument(teacher.documentNumber()),
                        teacher.name().sortableName(), Formats.text(teacher.specialization()),
                        teacher.status().dbValue()));
    }

    private void registerTeacher() {
        console.printTitle("Registrar profesor");
        support.show(controller.registerTeacher(readTeacher(null)));
        console.pause();
    }

    private void updateTeacher() {
        console.printTitle("Actualizar profesor");
        Optional<Teacher> selected = pickTeacher();
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        support.show(controller.updateTeacher(readTeacher(selected.get())));
        console.pause();
    }

    private Teacher readTeacher(Teacher current) {
        PersonDocumentType documentType = support.readEnum("Tipo de documento", PersonDocumentType.class);
        String document = console.readValid(prompt("Número de documento", current == null ? null
                : current.documentNumber()), raw -> raw.isBlank() && current != null
                ? current.documentNumber() : InputValidator.validateDocumentNumber(raw));
        PersonName name = support.readName(current == null ? null : current.name());
        String email = console.readValid(prompt("Correo electrónico", current == null ? null : current.email()),
                raw -> raw.isBlank() && current != null
                        ? current.email() : InputValidator.validateEmail(raw, "correo electrónico"));
        String phone = support.readOptional("Teléfono", current == null ? null : current.phone(),
                raw -> InputValidator.validatePhone(raw, "teléfono"));
        String specialization = support.readOptional("Especialización", current == null ? null
                : current.specialization(), raw -> InputValidator.requireText(raw, "especialización", 100));
        LocalDate hireDate = console.readValid(prompt("Fecha de contratación (AAAA-MM-DD)",
                current == null ? null : current.hireDate().toString()), raw -> raw.isBlank() && current != null
                ? current.hireDate() : InputValidator.parseDate(raw, "fecha de contratación"));
        TeacherStatus status = current == null ? TeacherStatus.ACTIVE : support.readEnum("Estado", TeacherStatus.class);
        return new Teacher(current == null ? null : current.id(), documentType, document, name, email, phone,
                specialization, hireDate, status);
    }

    private void listTeachers() {
        console.printTitle("Profesores");
        String text = console.readLine("Filtrar por nombre, documento o especialización (Enter para todos): ");
        support.show(controller.searchTeachers(text)).ifPresent(teachers -> console.printTable(TEACHER_HEADERS,
                teachers.stream().map(teacher -> List.of(String.valueOf(teacher.id()),
                        Formats.maskDocument(teacher.documentNumber()), teacher.name().sortableName(),
                        Formats.text(teacher.specialization()), teacher.status().dbValue()))
                        .collect(Collectors.toList())));
        console.pause();
    }

    /** Permite elegir un grado y luego una de sus materias. */
    Optional<Subject> pickSubject(boolean onlyActive) {
        Optional<List<Grade>> grades = support.quiet(controller.findGrades());
        if (grades.isEmpty()) {
            return Optional.empty();
        }
        console.println("Grado de la materia:");
        Grade grade = console.readChoice("Opción: ", grades.get(), Grade::name, "grado");
        Optional<List<Subject>> subjects = support.quiet(controller.findSubjectsByGrade(grade.id(), onlyActive));
        if (subjects.isEmpty() || subjects.get().isEmpty()) {
            console.printWarning("El grado " + grade.name() + " no tiene materias registradas.");
            return Optional.empty();
        }
        console.println("Materia:");
        return Optional.of(console.readChoice("Opción: ", subjects.get(),
                subject -> subject.code() + " - " + subject.name() + (subject.active() ? "" : " (inactiva)"),
                "materia"));
    }

    private void assign() {
        console.printTitle("Asignar materia");
        Optional<Teacher> teacher = pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Subject> subject = pickSubject(true);
        if (subject.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        String section = support.readSection("Sección o grupo");
        support.show(controller.assign(teacher.get().id(), subject.get().id(), year, section));
        console.pause();
    }

    private void listAssignments() {
        console.printTitle("Asignaciones");
        Optional<Teacher> teacher = pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(controller.findAssignments(teacher.get().id(), year)).ifPresent(this::printAssignments);
        console.pause();
    }

    private void printAssignments(List<TeacherAssignment> assignments) {
        console.printTable(List.of("Id", "Materia", "Año", "Sección"), assignments.stream()
                .map(assignment -> List.of(String.valueOf(assignment.id()), assignment.subjectName(),
                        String.valueOf(assignment.academicYear()), Formats.text(assignment.section())))
                .collect(Collectors.toList()));
    }

    private Optional<TeacherAssignment> pickAssignment() {
        Optional<Teacher> teacher = pickTeacher();
        if (teacher.isEmpty()) {
            return Optional.empty();
        }
        int year = support.readYear("Año académico");
        Optional<List<TeacherAssignment>> assignments = support.quiet(controller.findAssignments(teacher.get().id(), year));
        if (assignments.isEmpty() || assignments.get().isEmpty()) {
            console.printWarning("El profesor no tiene asignaciones en " + year + ".");
            return Optional.empty();
        }
        console.println("Asignación:");
        return Optional.of(console.readChoice("Opción: ", assignments.get(),
                assignment -> assignment.subjectName() + (assignment.section() == null ? "" : " - " + assignment.section()),
                "asignación"));
    }

    private void addSchedule() {
        console.printTitle("Registrar horario de clase");
        Optional<TeacherAssignment> assignment = pickAssignment();
        if (assignment.isEmpty()) {
            support.notFound();
            return;
        }
        WeekDay day = support.readEnum("Día", WeekDay.class);
        LocalTime start = support.readTime("Hora de inicio");
        LocalTime end = support.readTime("Hora de fin");
        String room = support.readOptional("Aula", null, raw -> InputValidator.requireText(raw, "aula", 50));
        support.show(controller.addSchedule(assignment.get().id(), day, start, end, room));
        console.pause();
    }

    private void showTimetable() {
        console.printTitle("Horario semanal");
        Optional<Teacher> teacher = pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        support.show(controller.findTimetable(teacher.get().id(), year)).ifPresent(this::printSchedules);
        console.pause();
    }

    private void printSchedules(List<ClassSchedule> schedules) {
        console.printTable(List.of("Id", "Día", "Inicio", "Fin", "Clase", "Aula"), schedules.stream()
                .map(slot -> List.of(String.valueOf(slot.id()), slot.day().dbValue(), slot.startTime().toString(),
                        slot.endTime().toString(), slot.description(), Formats.text(slot.room())))
                .collect(Collectors.toList()));
    }

    private void removeSchedule() {
        console.printTitle("Eliminar bloque de horario");
        Optional<Teacher> teacher = pickTeacher();
        if (teacher.isEmpty()) {
            support.notFound();
            return;
        }
        int year = support.readYear("Año académico");
        Optional<List<ClassSchedule>> schedules = support.quiet(controller.findTimetable(teacher.get().id(), year));
        if (schedules.isEmpty() || schedules.get().isEmpty()) {
            console.printWarning("El profesor no tiene horarios en " + year + ".");
            console.pause();
            return;
        }
        printSchedules(schedules.get());
        int scheduleId = console.readId("Id del bloque a eliminar: ", "id");
        boolean listed = schedules.get().stream().anyMatch(slot -> slot.id() == scheduleId);
        if (!listed) {
            console.printWarning("El id " + scheduleId + " no está en el horario mostrado.");
        } else if (console.confirm("¿Eliminar el bloque " + scheduleId + "?")) {
            support.show(controller.removeSchedule(scheduleId));
        }
        console.pause();
    }

    private static String prompt(String label, String current) {
        return current == null ? label + ": " : label + " [" + current + "]: ";
    }
}
