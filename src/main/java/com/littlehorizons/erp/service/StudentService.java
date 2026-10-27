package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.common.ImportResult;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.model.report.StudentProfile;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.util.TextFileReader;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business rules for students: registration, updates, search and withdrawal.
 */
public class StudentService {

    static final int MIN_AGE = 2;
    static final int MAX_AGE = 25;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int SEARCH_LIMIT = 100;
    private static final Pattern BLOOD_TYPE = Pattern.compile("^(A|B|AB|O)[+-]$");
    private static final int IMPORT_FIELDS = 10;
    private static final String SCHOOL_NAME = "COLEGIO INTERNACIONAL LITTLE HORIZONS";

    private final TransactionManager transactions;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final Clock clock;

    public StudentService(TransactionManager transactions, StudentDao studentDao, GuardianDao guardianDao,
                          EnrollmentDao enrollmentDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.guardianDao = Objects.requireNonNull(guardianDao, "guardianDao");
        this.enrollmentDao = Objects.requireNonNull(enrollmentDao, "enrollmentDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Validates and inserts a new student with status "Activo". */
    public Student register(Student draft) {
        Student student = normalize(draft).withStatus(StudentStatus.ACTIVE);
        validate(student);
        return transactions.inTransaction(connection -> {
            ensureDocumentIsFree(connection, student.documentNumber(), null);
            return student.withId(studentDao.insert(connection, student));
        });
    }

    /** Updates personal data; the status is changed only through {@link #withdraw}. */
    public Student update(Student changes) {
        Objects.requireNonNull(changes.id(), "id");
        Student student = normalize(changes);
        validate(student);
        return transactions.inTransaction(connection -> {
            Student current = ServiceChecks.require(studentDao.findById(connection, student.id()),
                    "No existe un estudiante con id " + student.id() + ".");
            ensureDocumentIsFree(connection, student.documentNumber(), student.id());
            Student updated = student.withStatus(current.status());
            studentDao.update(connection, updated);
            return updated;
        });
    }

    /**
     * Withdraws a student: status "Retirado" and, if there is an active
     * enrollment in the year of {@code date}, it becomes "Retirada" with that
     * date. Both changes are one transaction. History is kept.
     */
    public Student withdraw(int studentId, LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        ServiceChecks.notInFuture(date, today, "fecha de retiro");
        return transactions.inTransaction(connection -> {
            Student student = findOrFail(connection, studentId);
            if (student.status() == StudentStatus.WITHDRAWN) {
                throw new BusinessRuleException("El estudiante ya se encuentra retirado.");
            }
            studentDao.updateStatus(connection, studentId, StudentStatus.WITHDRAWN);
            Optional<Enrollment> activeEnrollment = enrollmentDao
                    .findByStudentAndYear(connection, studentId, date.getYear())
                    .filter(enrollment -> enrollment.status() == EnrollmentStatus.ACTIVE);
            if (activeEnrollment.isPresent()) {
                Enrollment enrollment = activeEnrollment.get();
                if (date.isBefore(enrollment.enrollmentDate())) {
                    throw new ValidationException("La fecha de retiro no puede ser anterior a la matrícula ("
                            + enrollment.enrollmentDate() + ").");
                }
                enrollmentDao.updateStatus(connection, enrollment.id(), EnrollmentStatus.WITHDRAWN, date);
            }
            return student.withStatus(StudentStatus.WITHDRAWN);
        });
    }

    /** Changes the status to Activo, Graduado or Suspendido (Retirado uses {@link #withdraw}). */
    public Student changeStatus(int studentId, StudentStatus status) {
        if (status == StudentStatus.WITHDRAWN) {
            throw new ValidationException("Para retirar a un estudiante use la opción de retiro.");
        }
        return transactions.inTransaction(connection -> {
            Student student = findOrFail(connection, studentId);
            studentDao.updateStatus(connection, studentId, status);
            return student.withStatus(status);
        });
    }

    public Student findById(int studentId) {
        return transactions.read(connection -> findOrFail(connection, studentId));
    }

    public Student findByDocument(String documentNumber) {
        String document = InputValidator.validateDocumentNumber(documentNumber);
        return transactions.read(connection -> ServiceChecks.require(studentDao.findByDocument(connection, document),
                "No existe un estudiante con el documento " + document + "."));
    }

    public List<Student> search(String text) {
        return transactions.read(connection -> studentDao.search(connection, text, SEARCH_LIMIT));
    }

    /** Student with guardians and enrollment history, read in one connection. */
    public StudentProfile getProfile(int studentId) {
        return transactions.read(connection -> {
            Student student = findOrFail(connection, studentId);
            List<Enrollment> enrollments = enrollmentDao.findByStudent(connection, studentId);
            return new StudentProfile(student, guardianDao.findGuardiansOfStudent(connection, studentId),
                    enrollments, guardianDao.findSiblings(connection, studentId));
        });
    }

    // ------------------------------------------------------ text file exercises

    /**
     * Exercise 3: study certificate in exports/constancias. Only for an active
     * student with an active enrollment in the current year.
     */
    public Path exportCertificate(int studentId) {
        LocalDate today = LocalDate.now(clock);
        StudentProfile profile = getProfile(studentId);
        Student student = profile.student();
        if (student.status() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("No se puede expedir la constancia: el estudiante está "
                    + student.status().dbValue() + ".");
        }
        Enrollment enrollment = profile.enrollments().stream()
                .filter(e -> e.academicYear() == today.getYear() && e.status() == EnrollmentStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se puede expedir la constancia: el estudiante "
                        + "no tiene matrícula activa en " + today.getYear() + "."));

        List<String> lines = List.of(
                SCHOOL_NAME,
                "Documento sin validez oficial (ejercicio académico)",
                "",
                "CONSTANCIA DE ESTUDIO",
                "",
                "El colegio Little Horizons hace constar que " + student.fullName().toUpperCase(),
                "identificado(a) con " + student.documentType().dbValue() + " " + student.documentNumber() + ",",
                "se encuentra matriculado(a) en el grado " + enrollment.gradeName()
                        + " para el año académico " + enrollment.academicYear() + ".",
                "",
                "Expedida el " + Formats.longDate(today) + ".",
                "",
                "",
                "______________________________",
                "Secretaría académica");
        Path file = FileSupport.EXPORTS.resolve("constancias")
                .resolve("constancia_" + student.documentNumber() + ".txt");
        return FileSupport.write(file, lines, StudentService.class);
    }

    /**
     * Exercise 4: full record in exports/fichas. Medical data only when
     * {@code includeMedical} is true (the view asks first).
     */
    public Path exportProfile(int studentId, boolean includeMedical) {
        LocalDate today = LocalDate.now(clock);
        StudentProfile profile = getProfile(studentId);
        Student student = profile.student();
        List<String> lines = new ArrayList<>();
        lines.add(SCHOOL_NAME);
        lines.add("FICHA DEL ESTUDIANTE - generada el " + Formats.date(today));
        lines.add("=".repeat(60));
        lines.add(field("Id", String.valueOf(student.id())));
        lines.add(field("Documento", student.documentType().dbValue() + " " + student.documentNumber()));
        lines.add(field("Nombre", student.fullName()));
        lines.add(field("Nacimiento", Formats.date(student.birthDate()) + " (" + student.ageOn(today) + " años)"));
        lines.add(field("Género", student.gender().dbValue()));
        lines.add(field("Dirección", Formats.text(student.address())));
        lines.add(field("Teléfono", Formats.text(student.phone())));
        lines.add(field("Correo", Formats.text(student.email())));
        lines.add(field("Ingreso", Formats.date(student.admissionDate())));
        lines.add(field("Estado", student.status().dbValue()));
        if (includeMedical) {
            lines.add("");
            lines.add("DATOS MÉDICOS (información sensible)");
            lines.add(field("Tipo de sangre", Formats.text(student.bloodType())));
            lines.add(field("Alergias", Formats.text(student.allergies())));
            lines.add(field("Condiciones", Formats.text(student.medicalConditions())));
        }
        lines.add("");
        lines.add("ACUDIENTES");
        if (profile.guardians().isEmpty()) {
            lines.add("  (sin acudientes registrados)");
        }
        for (GuardianLink link : profile.guardians()) {
            lines.add("  - " + link.guardian().fullName() + " (" + link.guardian().relationship() + "), tel. "
                    + link.guardian().mainPhone()
                    + (link.primary() ? " [principal]" : "")
                    + (link.emergencyContact() ? " [contacto de emergencia]" : ""));
        }
        lines.add("");
        lines.add("MATRÍCULAS");
        if (profile.enrollments().isEmpty()) {
            lines.add("  (sin matrículas)");
        }
        profile.enrollments().stream()
                .sorted(Comparator.comparingInt(Enrollment::academicYear))
                .forEach(e -> lines.add("  " + e.academicYear() + "  " + e.gradeName() + "  " + e.status().dbValue()
                        + "  desde " + Formats.date(e.enrollmentDate())));
        lines.add("");
        lines.add("HERMANOS EN EL COLEGIO");
        if (profile.siblings().isEmpty()) {
            lines.add("  (ninguno)");
        }
        profile.siblings().forEach(s -> lines.add("  - " + s.fullName() + " (id " + s.id() + ")"));
        Path file = FileSupport.EXPORTS.resolve("fichas").resolve("ficha_" + student.documentNumber() + ".txt");
        return FileSupport.write(file, lines, StudentService.class);
    }

    /**
     * Exercise 15: registers one student per line:
     * tipoDoc;documento;primerNombre;segundoNombre;primerApellido;segundoApellido;fechaNacimiento;genero;direccion;telefono.
     * A bad line is reported and the rest continue; the admission date is today.
     */
    public ImportResult importFromFile(Path file) {
        List<TextFileReader.Line> lines = FileSupport.readDataLines(file, StudentService.class);
        List<String> errors = new ArrayList<>();
        int saved = 0;
        for (TextFileReader.Line line : lines) {
            try {
                register(toStudent(line.fields()));
                saved++;
            } catch (ValidationException | BusinessRuleException e) {
                errors.add(FileSupport.lineError(line.number(), e.getMessage()));
            }
        }
        return new ImportResult(lines.size(), saved, errors);
    }

    private Student toStudent(String[] f) {
        if (f.length < IMPORT_FIELDS) {
            throw new ValidationException("se esperaban " + IMPORT_FIELDS + " campos separados por ';' y hay "
                    + f.length + ".");
        }
        StudentDocumentType documentType = FileSupport.parseEnum(f[0], StudentDocumentType.class, "tipo de documento");
        LocalDate birthDate = InputValidator.parseDate(f[6], "fecha de nacimiento");
        Gender gender = FileSupport.parseEnum(f[7], Gender.class, "género");
        if (f[2].isEmpty() || f[4].isEmpty()) {
            throw new ValidationException("el primer nombre y el primer apellido son obligatorios.");
        }
        PersonName name = new PersonName(f[2], blankToNull(f[3]), f[4], blankToNull(f[5]));
        return new Student(null, documentType, f[1], name, birthDate, gender, blankToNull(f[8]),
                blankToNull(f[9]), null, null, null, null, LocalDate.now(clock), StudentStatus.ACTIVE);
    }

    private static String field(String label, String value) {
        return String.format("%-16s %s", label + ":", value);
    }

    private Student findOrFail(Connection connection, int studentId) throws SQLException {
        return ServiceChecks.require(studentDao.findById(connection, studentId),
                "No existe un estudiante con id " + studentId + ".");
    }

    private void ensureDocumentIsFree(Connection connection, String documentNumber, Integer ownId)
            throws SQLException {
        studentDao.findByDocument(connection, documentNumber)
                .filter(existing -> !existing.id().equals(ownId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Ya existe un estudiante con el documento " + documentNumber
                            + " (" + existing.fullName() + ").");
                });
    }

    private Student normalize(Student student) {
        return new Student(student.id(), student.documentType(),
                InputValidator.validateDocumentNumber(student.documentNumber()),
                student.name(), student.birthDate(), student.gender(), blankToNull(student.address()),
                blankToNull(student.phone()), student.email() == null || student.email().isBlank()
                        ? null : InputValidator.validateEmail(student.email(), "correo electrónico"),
                student.bloodType() == null || student.bloodType().isBlank()
                        ? null : student.bloodType().strip().toUpperCase(java.util.Locale.ROOT),
                blankToNull(student.allergies()), blankToNull(student.medicalConditions()),
                student.admissionDate(), student.status());
    }

    private void validate(Student student) {
        LocalDate today = LocalDate.now(clock);
        if (student.documentType() == null || student.gender() == null) {
            throw new ValidationException("El tipo de documento y el género son obligatorios.");
        }
        ServiceChecks.requireText(student.name().firstName(), "primer nombre", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(student.name().middleName(), "segundo nombre", MAX_NAME_LENGTH);
        ServiceChecks.requireText(student.name().lastName(), "primer apellido", MAX_NAME_LENGTH);
        ServiceChecks.optionalText(student.name().secondLastName(), "segundo apellido", MAX_NAME_LENGTH);
        ServiceChecks.notInFuture(student.birthDate(), today, "fecha de nacimiento");
        ServiceChecks.notInFuture(student.admissionDate(), today, "fecha de ingreso");
        int ageAtAdmission = student.ageOn(student.admissionDate());
        if (ageAtAdmission < MIN_AGE || ageAtAdmission > MAX_AGE) {
            throw new ValidationException("La edad al ingreso debe estar entre " + MIN_AGE + " y " + MAX_AGE
                    + " años; revise las fechas de nacimiento e ingreso.");
        }
        ServiceChecks.optionalText(student.address(), "dirección", MAX_ADDRESS_LENGTH);
        ServiceChecks.optionalPhone(student.phone(), "teléfono");
        if (student.bloodType() != null && !BLOOD_TYPE.matcher(student.bloodType()).matches()) {
            throw new ValidationException("El tipo de sangre debe ser A, B, AB u O seguido de + o - (ejemplo: O+).");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
