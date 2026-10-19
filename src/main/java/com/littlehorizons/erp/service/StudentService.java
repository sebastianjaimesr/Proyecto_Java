package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.report.StudentProfile;
import com.littlehorizons.erp.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de los estudiantes: registro, actualización, búsqueda y retiro.
 */
public class StudentService {

    static final int MIN_AGE = 2;
    static final int MAX_AGE = 25;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int SEARCH_LIMIT = 100;
    private static final Pattern BLOOD_TYPE = Pattern.compile("^(A|B|AB|O)[+-]$");

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

    /** Valida e inserta un estudiante nuevo con estado "Activo". */
    public Student register(Student draft) {
        Student student = normalize(draft).withStatus(StudentStatus.ACTIVE);
        validate(student);
        return transactions.inTransaction(connection -> {
            ensureDocumentIsFree(connection, student.documentNumber(), null);
            return student.withId(studentDao.insert(connection, student));
        });
    }

    /** Actualiza los datos personales; el estado solo se cambia a través de {@link #withdraw}. */
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
     * Retira a un estudiante: estado "Retirado" y, si tiene una matrícula
     * activa en el año de {@code date}, pasa a "Retirada" con esa
     * fecha. Ambos cambios son una sola transacción. El historial se conserva.
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

    /** Cambia el estado a Activo, Graduado o Suspendido (Retirado usa {@link #withdraw}). */
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

    /** Busca un estudiante por id o lanza un error si no existe. */
    public Student findById(int studentId) {
        return transactions.read(connection -> findOrFail(connection, studentId));
    }

    /** Busca un estudiante por número de documento o lanza un error si no existe. */
    public Student findByDocument(String documentNumber) {
        String document = InputValidator.validateDocumentNumber(documentNumber);
        return transactions.read(connection -> ServiceChecks.require(studentDao.findByDocument(connection, document),
                "No existe un estudiante con el documento " + document + "."));
    }

    /** Busca estudiantes por nombre o documento (con un límite de resultados). */
    public List<Student> search(String text) {
        return transactions.read(connection -> studentDao.search(connection, text, SEARCH_LIMIT));
    }

    /** Estudiante con sus acudientes e historial de matrículas, leído con una sola conexión. */
    public StudentProfile getProfile(int studentId) {
        return transactions.read(connection -> {
            Student student = findOrFail(connection, studentId);
            List<Enrollment> enrollments = enrollmentDao.findByStudent(connection, studentId);
            return new StudentProfile(student, guardianDao.findGuardiansOfStudent(connection, studentId),
                    enrollments, guardianDao.findSiblings(connection, studentId));
        });
    }

    /** Busca el estudiante o lanza un error si no existe. */
    private Student findOrFail(Connection connection, int studentId) throws SQLException {
        return ServiceChecks.require(studentDao.findById(connection, studentId),
                "No existe un estudiante con id " + studentId + ".");
    }

    /**
     * Verifica que ningún otro estudiante tenga el mismo documento ({@code ownId} es el propio, al actualizar).
     */
    private void ensureDocumentIsFree(Connection connection, String documentNumber, Integer ownId)
            throws SQLException {
        studentDao.findByDocument(connection, documentNumber)
                .filter(existing -> !existing.id().equals(ownId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Ya existe un estudiante con el documento " + documentNumber
                            + " (" + existing.fullName() + ").");
                });
    }

    /** Limpia los datos: documento en mayúsculas, textos sin espacios sobrantes y vacíos como null. */
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

    /**
     * Valida los datos del estudiante: campos obligatorios, fechas no futuras, edad al ingreso
     * dentro del rango permitido, teléfono y tipo de sangre (A, B, AB u O con + o -).
     */
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

    /** Convierte un texto vacío en null; si no, lo devuelve sin espacios a los lados. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
