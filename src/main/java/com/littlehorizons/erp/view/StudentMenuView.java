package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentLink;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.model.report.StudentProfile;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pantallas de "1. Gestión de estudiantes y acudientes".
 *
 * <p>Las listas muestran documentos enmascarados y ningún dato médico; el registro completo
 * se muestra solo en la pantalla de detalle, y los datos médicos solo después de confirmar.</p>
 */
public class StudentMenuView {

    private static final List<String> STUDENT_HEADERS = List.of("Id", "Documento", "Nombre", "Estado");
    private static final List<String> GUARDIAN_HEADERS = List.of("Id", "Documento", "Nombre", "Parentesco", "Teléfono");

    private final ConsoleUtils console;
    private final ViewSupport support;
    private final StudentController controller;

    public StudentMenuView(ViewSupport support, StudentController controller) {
        this.support = Objects.requireNonNull(support, "support");
        this.console = support.console();
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /** Muestra el submenú de "Gestión de estudiantes y acudientes". */
    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Registrar estudiante", this::registerStudent),
                        new Menu.Item("Consultar ficha de un estudiante", this::showProfile),
                        new Menu.Item("Actualizar datos de un estudiante", this::updateStudent),
                        new Menu.Item("Cambiar estado o retirar un estudiante", this::changeStatus),
                        new Menu.Item("Registrar acudiente para un estudiante", this::registerGuardianForStudent),
                        new Menu.Item("Relacionar un acudiente existente con un estudiante", this::linkGuardian),
                        new Menu.Item("Quitar la relación con un acudiente", this::unlinkGuardian),
                        new Menu.Item("Actualizar datos de un acudiente", this::updateGuardian),
                        new Menu.Item("Consultar acudiente y estudiantes a cargo", this::showGuardian),
                        new Menu.Item("Matricular estudiante", this::enroll),
                        new Menu.Item("Cerrar una matrícula (completada o retirada)", this::closeEnrollment)),
                "GESTIÓN DE ESTUDIANTES Y ACUDIENTES").run();
    }

    // ------------------------------------------------------------- estudiantes

    /** Selector de estudiante compartido; también lo usan las vistas de otros módulos. */
    Optional<Student> pickStudent() {
        return support.pick("estudiante", controller::searchStudents, Student::id, STUDENT_HEADERS,
                student -> List.of(String.valueOf(student.id()), Formats.maskDocument(student.documentNumber()),
                        student.name().sortableName(), student.status().dbValue()));
    }

    /** Pantalla para registrar un estudiante. */
    private void registerStudent() {
        console.printTitle("Registrar estudiante");
        Student draft = readStudent(null);
        support.show(controller.registerStudent(draft));
        console.pause();
    }

    /** Pantalla para actualizar los datos de un estudiante. */
    private void updateStudent() {
        console.printTitle("Actualizar estudiante");
        Optional<Student> selected = pickStudent();
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        Student changes = readStudent(selected.get());
        support.show(controller.updateStudent(changes));
        console.pause();
    }

    /** Pide los datos de un estudiante; si {@code current} existe, Enter conserva cada valor actual. */
    private Student readStudent(Student current) {
        StudentDocumentType documentType = support.readEnum("Tipo de documento", StudentDocumentType.class);
        String document = console.readValid(prompt("Número de documento", current == null ? null
                : current.documentNumber()), raw -> raw.isBlank() && current != null
                ? current.documentNumber() : InputValidator.validateDocumentNumber(raw));
        PersonName name = support.readName(current == null ? null : current.name());
        LocalDate birthDate = console.readValid(prompt("Fecha de nacimiento (AAAA-MM-DD)",
                current == null ? null : current.birthDate().toString()), raw -> raw.isBlank() && current != null
                ? current.birthDate() : InputValidator.parseDate(raw, "fecha de nacimiento"));
        Gender gender = support.readEnum("Género", Gender.class);
        String address = support.readOptional("Dirección", current == null ? null : current.address(),
                raw -> InputValidator.requireText(raw, "dirección", 255));
        String phone = support.readOptional("Teléfono", current == null ? null : current.phone(),
                raw -> InputValidator.validatePhone(raw, "teléfono"));
        String email = support.readOptional("Correo electrónico", current == null ? null : current.email(),
                raw -> InputValidator.validateEmail(raw, "correo electrónico"));
        String bloodType = support.readOptional("Tipo de sangre (ej: O+)", current == null ? null : current.bloodType(),
                raw -> InputValidator.requireText(raw, "tipo de sangre", 5));
        String allergies = support.readOptional("Alergias", current == null ? null : current.allergies(),
                raw -> InputValidator.requireText(raw, "alergias", 2000));
        String medical = support.readOptional("Condiciones médicas", current == null ? null
                : current.medicalConditions(), raw -> InputValidator.requireText(raw, "condiciones médicas", 2000));
        LocalDate admission = console.readValid(prompt("Fecha de ingreso al colegio (AAAA-MM-DD)",
                current == null ? null : current.admissionDate().toString()), raw -> raw.isBlank() && current != null
                ? current.admissionDate() : InputValidator.parseDate(raw, "fecha de ingreso"));
        return new Student(current == null ? null : current.id(), documentType, document, name, birthDate, gender,
                address, phone, email, bloodType, allergies, medical, admission,
                current == null ? StudentStatus.ACTIVE : current.status());
    }

    /** Muestra la ficha de un estudiante; los datos médicos solo si el usuario confirma. */
    private void showProfile() {
        console.printTitle("Ficha del estudiante");
        Optional<Student> selected = pickStudent();
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        support.quiet(controller.getStudentProfile(selected.get().id())).ifPresent(this::printProfile);
        console.pause();
    }

    /** Imprime la ficha: datos personales, acudientes, matrículas y hermanos. */
    private void printProfile(StudentProfile profile) {
        Student student = profile.student();
        console.printTable(List.of("Campo", "Valor"), List.of(
                List.of("Id", String.valueOf(student.id())),
                List.of("Documento", student.documentType().dbValue() + " " + student.documentNumber()),
                List.of("Nombre completo", student.fullName()),
                List.of("Fecha de nacimiento", Formats.date(student.birthDate())
                        + " (" + student.ageOn(LocalDate.now()) + " años)"),
                List.of("Género", student.gender().dbValue()),
                List.of("Dirección", Formats.text(student.address())),
                List.of("Teléfono", Formats.text(student.phone())),
                List.of("Correo", Formats.text(student.email())),
                List.of("Fecha de ingreso", Formats.date(student.admissionDate())),
                List.of("Estado", student.status().dbValue())));
        console.println("Acudientes y contactos de emergencia:");
        if (profile.guardians().isEmpty()) {
            console.printWarning("El estudiante no tiene acudientes registrados.");
        } else {
            console.printTable(List.of("Id", "Nombre", "Parentesco", "Teléfono", "Principal", "Emergencia",
                            "Resp. financiero"),
                    profile.guardians().stream().map(link -> List.of(String.valueOf(link.guardian().id()),
                            link.guardian().fullName(), link.guardian().relationship(), link.guardian().mainPhone(),
                            Formats.yesNo(link.primary()), Formats.yesNo(link.emergencyContact()),
                            Formats.yesNo(link.guardian().financialResponsible()))).collect(Collectors.toList()));
        }
        console.println("Matrículas:");
        console.printTable(List.of("Id", "Año", "Grado", "Estado", "Fecha", "Retiro"),
                profile.enrollments().stream().map(enrollment -> List.of(String.valueOf(enrollment.id()),
                        String.valueOf(enrollment.academicYear()), enrollment.gradeName(),
                        enrollment.status().dbValue(), Formats.date(enrollment.enrollmentDate()),
                        Formats.date(enrollment.withdrawalDate()))).collect(Collectors.toList()));
        if (!profile.siblings().isEmpty()) {
            console.println("Hermanos en el colegio (comparten acudiente): " + profile.siblings().stream()
                    .map(sibling -> sibling.fullName() + " (id " + sibling.id() + ")")
                    .collect(Collectors.joining(", ")));
        }
        if (console.confirm("¿Mostrar datos médicos (información sensible)?")) {
            console.printTable(List.of("Dato médico", "Valor"), List.of(
                    List.of("Tipo de sangre", Formats.text(student.bloodType())),
                    List.of("Alergias", Formats.text(student.allergies())),
                    List.of("Condiciones médicas", Formats.text(student.medicalConditions()))));
        }
    }

    /** Cambia el estado de un estudiante o lo retira. */
    private void changeStatus() {
        console.printTitle("Cambiar estado o retirar");
        Optional<Student> selected = pickStudent();
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        Student student = selected.get();
        console.println("Estado actual: " + student.status().dbValue());
        StudentStatus status = support.readEnum("Nuevo estado", StudentStatus.class);
        if (status == StudentStatus.WITHDRAWN) {
            LocalDate date = support.readDateOrToday("Fecha de retiro");
            if (console.confirm("El retiro también cierra la matrícula activa de " + date.getYear()
                    + ". No se borra información. ¿Continuar?")) {
                support.show(controller.withdrawStudent(student.id(), date));
            } else {
                support.notFound();
            }
        } else {
            support.show(controller.changeStudentStatus(student.id(), status));
        }
        console.pause();
    }

    // ------------------------------------------------------------ acudientes

    /** Busca acudientes y deja elegir uno (también lo usan otras vistas). */
    Optional<Guardian> pickGuardian() {
        return support.pick("acudiente", controller::searchGuardians, Guardian::id, GUARDIAN_HEADERS,
                guardian -> List.of(String.valueOf(guardian.id()), Formats.maskDocument(guardian.documentNumber()),
                        guardian.name().sortableName(), guardian.relationship(), guardian.mainPhone()));
    }

    /** Pide los datos de un acudiente; si {@code current} existe, Enter conserva cada valor actual. */
    private Guardian readGuardian(Guardian current) {
        PersonDocumentType documentType = support.readEnum("Tipo de documento", PersonDocumentType.class);
        String document = console.readValid(prompt("Número de documento", current == null ? null
                : current.documentNumber()), raw -> raw.isBlank() && current != null
                ? current.documentNumber() : InputValidator.validateDocumentNumber(raw));
        PersonName name = support.readName(current == null ? null : current.name());
        String relationship = support.readTextOrKeep("Parentesco (Padre, Madre, Abuelo, Tío, Tutor Legal...)",
                current == null ? null : current.relationship(), true);
        String mainPhone = console.readValid(prompt("Teléfono principal", current == null ? null
                : current.mainPhone()), raw -> raw.isBlank() && current != null
                ? current.mainPhone() : InputValidator.validatePhone(raw, "teléfono principal"));
        String secondaryPhone = support.readOptional("Teléfono secundario",
                current == null ? null : current.secondaryPhone(),
                raw -> InputValidator.validatePhone(raw, "teléfono secundario"));
        String email = support.readOptional("Correo electrónico", current == null ? null : current.email(),
                raw -> InputValidator.validateEmail(raw, "correo electrónico"));
        String address = support.readOptional("Dirección", current == null ? null : current.address(),
                raw -> InputValidator.requireText(raw, "dirección", 255));
        String occupation = support.readOptional("Ocupación", current == null ? null : current.occupation(),
                raw -> InputValidator.requireText(raw, "ocupación", 100));
        boolean financial = console.confirm("¿Es responsable financiero (paga y recibe facturas)?");
        return new Guardian(current == null ? null : current.id(), documentType, document, name, relationship,
                mainPhone, secondaryPhone, email, address, occupation, financial);
    }

    /** Registra un acudiente nuevo y lo vincula al estudiante elegido. */
    private void registerGuardianForStudent() {
        console.printTitle("Registrar acudiente");
        Optional<Student> student = pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Guardian draft = readGuardian(null);
        boolean primary = console.confirm("¿Es el acudiente principal para comunicaciones?");
        boolean emergency = console.confirm("¿Es contacto de emergencia?");
        support.show(controller.registerGuardianForStudent(draft, student.get().id(), primary, emergency));
        console.pause();
    }

    /** Vincula un acudiente ya registrado a un estudiante. */
    private void linkGuardian() {
        console.printTitle("Relacionar acudiente existente");
        Optional<Student> student = pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<Guardian> guardian = pickGuardian();
        if (guardian.isEmpty()) {
            support.notFound();
            return;
        }
        boolean primary = console.confirm("¿Es el acudiente principal para comunicaciones?");
        boolean emergency = console.confirm("¿Es contacto de emergencia?");
        support.show(controller.linkGuardian(student.get().id(), guardian.get().id(), primary, emergency));
        console.pause();
    }

    /** Quita la relación entre un estudiante y un acudiente. */
    private void unlinkGuardian() {
        console.printTitle("Quitar relación con acudiente");
        Optional<Student> student = pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<List<GuardianLink>> links = support.quiet(controller.findGuardiansOfStudent(student.get().id()));
        if (links.isEmpty() || links.get().isEmpty()) {
            console.printWarning("El estudiante no tiene acudientes.");
            console.pause();
            return;
        }
        console.printTable(List.of("Id", "Nombre", "Parentesco"), links.get().stream()
                .map(link -> List.of(String.valueOf(link.guardian().id()), link.guardian().fullName(),
                        link.guardian().relationship())).collect(Collectors.toList()));
        int guardianId = console.readId("Id del acudiente: ", "id");
        if (console.confirm("¿Quitar la relación? El acudiente y sus pagos no se borran.")) {
            support.show(controller.unlinkGuardian(student.get().id(), guardianId));
        }
        console.pause();
    }

    /** Pantalla para actualizar los datos de un acudiente. */
    private void updateGuardian() {
        console.printTitle("Actualizar acudiente");
        Optional<Guardian> guardian = pickGuardian();
        if (guardian.isEmpty()) {
            support.notFound();
            return;
        }
        console.printInfo("Presione Enter para conservar el valor actual entre corchetes.");
        support.show(controller.updateGuardian(readGuardian(guardian.get())));
        console.pause();
    }

    /** Muestra los datos de un acudiente y los estudiantes a su cargo. */
    private void showGuardian() {
        console.printTitle("Consultar acudiente");
        Optional<Guardian> selected = pickGuardian();
        if (selected.isEmpty()) {
            support.notFound();
            return;
        }
        Guardian guardian = selected.get();
        console.printTable(List.of("Campo", "Valor"), List.of(
                List.of("Documento", guardian.documentType().dbValue() + " " + guardian.documentNumber()),
                List.of("Nombre", guardian.fullName()),
                List.of("Parentesco", guardian.relationship()),
                List.of("Teléfonos", guardian.mainPhone() + (guardian.secondaryPhone() == null
                        ? "" : " / " + guardian.secondaryPhone())),
                List.of("Correo", Formats.text(guardian.email())),
                List.of("Dirección", Formats.text(guardian.address())),
                List.of("Ocupación", Formats.text(guardian.occupation())),
                List.of("Responsable financiero", Formats.yesNo(guardian.financialResponsible()))));
        support.quiet(controller.findStudentsOfGuardian(guardian.id())).ifPresent(students -> {
            console.println("Estudiantes a cargo:");
            console.printTable(List.of("Id", "Nombre", "Estado", "Principal", "Emergencia"),
                    students.stream().map((StudentLink link) -> List.of(String.valueOf(link.student().id()),
                            link.student().fullName(), link.student().status().dbValue(),
                            Formats.yesNo(link.primary()), Formats.yesNo(link.emergencyContact())))
                            .collect(Collectors.toList()));
        });
        console.pause();
    }

    // ---------------------------------------------------------- matrículas

    /** Matricula a un estudiante en un grado para un año. */
    private void enroll() {
        console.printTitle("Matricular estudiante");
        Optional<Student> student = pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<List<Grade>> grades = support.quiet(controller.findGrades());
        if (grades.isEmpty()) {
            console.pause();
            return;
        }
        console.println("Grado:");
        Grade grade = console.readChoice("Opción: ", grades.get(), Grade::name, "grado");
        int year = support.readYear("Año académico");
        LocalDate date = support.readDateOrToday("Fecha de matrícula");
        String notes = support.readOptional("Observaciones", null, raw -> InputValidator.requireText(raw,
                "observaciones", 1000));
        support.show(controller.enroll(student.get().id(), grade.id(), year, date, notes));
        console.pause();
    }

    /** Cierra la matrícula activa de un estudiante como completada o retirada. */
    private void closeEnrollment() {
        console.printTitle("Cerrar matrícula");
        Optional<Student> student = pickStudent();
        if (student.isEmpty()) {
            support.notFound();
            return;
        }
        Optional<List<Enrollment>> enrollments = support.quiet(controller.enrollmentHistory(student.get().id()));
        List<Enrollment> active = enrollments.orElse(List.of()).stream()
                .filter(enrollment -> enrollment.status() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());
        if (active.isEmpty()) {
            console.printWarning("El estudiante no tiene matrículas activas.");
            console.pause();
            return;
        }
        Enrollment enrollment = console.readChoice("Matrícula: ", active,
                item -> item.academicYear() + " - " + item.gradeName(), "matrícula");
        EnrollmentStatus status = support.readEnum("Nuevo estado",
                List.of(EnrollmentStatus.COMPLETED, EnrollmentStatus.WITHDRAWN));
        LocalDate date = support.readDateOrToday("Fecha");
        support.show(controller.closeEnrollment(enrollment.id(), status, date));
        console.pause();
    }

    /** Texto de la pregunta; si hay un valor actual, lo muestra entre corchetes (Enter lo conserva). */
    private static String prompt(String label, String current) {
        return current == null ? label + ": " : label + " [" + current + "]: ";
    }
}
