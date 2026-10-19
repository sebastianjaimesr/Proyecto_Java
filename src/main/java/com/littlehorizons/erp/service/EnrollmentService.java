package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.CatalogDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Matrículas anuales: una por estudiante y año académico, de
 * Preescolar a Once.
 */
public class EnrollmentService {

    private static final int MAX_NOTES_LENGTH = 1000;

    private final TransactionManager transactions;
    private final EnrollmentDao enrollmentDao;
    private final StudentDao studentDao;
    private final CatalogDao catalogDao;
    private final Clock clock;

    public EnrollmentService(TransactionManager transactions, EnrollmentDao enrollmentDao, StudentDao studentDao,
                             CatalogDao catalogDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.enrollmentDao = Objects.requireNonNull(enrollmentDao, "enrollmentDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.catalogDao = Objects.requireNonNull(catalogDao, "catalogDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Lista los grados del catálogo (Preescolar a Once). */
    public List<Grade> findGrades() {
        return transactions.read(catalogDao::findAllGrades);
    }

    /**
     * Matricula a un estudiante activo. La base de datos también rechaza una segunda
     * matrícula para el mismo año (UNIQUE matricula_unica).
     */
    public Enrollment enroll(int studentId, int gradeId, int academicYear, LocalDate enrollmentDate, String notes) {
        LocalDate today = LocalDate.now(clock);
        ServiceChecks.academicYear(academicYear, today);
        ServiceChecks.notInFuture(enrollmentDate, today, "fecha de matrícula");
        ServiceChecks.optionalText(notes, "observaciones", MAX_NOTES_LENGTH);
        return transactions.inTransaction(connection -> {
            Student student = ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            if (student.status() != StudentStatus.ACTIVE) {
                throw new BusinessRuleException("Solo se pueden matricular estudiantes en estado Activo (estado actual: "
                        + student.status().dbValue() + ").");
            }
            Grade grade = ServiceChecks.require(catalogDao.findGradeById(connection, gradeId),
                    "No existe un grado con id " + gradeId + ".");
            if (enrollmentDao.findByStudentAndYear(connection, studentId, academicYear).isPresent()) {
                throw new BusinessRuleException("El estudiante ya tiene una matrícula para el año " + academicYear + ".");
            }
            if (enrollmentDate.isBefore(student.admissionDate())) {
                throw new ValidationException("La fecha de matrícula no puede ser anterior a la fecha de ingreso ("
                        + student.admissionDate() + ").");
            }
            Enrollment draft = new Enrollment(null, studentId, gradeId, grade.name(), academicYear,
                    EnrollmentStatus.ACTIVE, enrollmentDate, null, notes);
            int id = enrollmentDao.insert(connection, draft);
            return new Enrollment(id, studentId, gradeId, grade.name(), academicYear, EnrollmentStatus.ACTIVE,
                    enrollmentDate, null, notes);
        });
    }

    /**
     * Cierra una matrícula activa como "Completada" o "Retirada". Una matrícula
     * cerrada es historial y no se puede reabrir.
     */
    public void close(int enrollmentId, EnrollmentStatus newStatus, LocalDate date) {
        if (newStatus == EnrollmentStatus.ACTIVE) {
            throw new ValidationException("Elija Completada o Retirada.");
        }
        ServiceChecks.notInFuture(date, LocalDate.now(clock), "fecha");
        transactions.inTransactionVoid(connection -> {
            Enrollment enrollment = ServiceChecks.require(enrollmentDao.findById(connection, enrollmentId),
                    "No existe una matrícula con id " + enrollmentId + ".");
            if (enrollment.status() != EnrollmentStatus.ACTIVE) {
                throw new BusinessRuleException("La matrícula ya está cerrada (" + enrollment.status().dbValue() + ").");
            }
            if (date.isBefore(enrollment.enrollmentDate())) {
                throw new ValidationException("La fecha no puede ser anterior a la matrícula.");
            }
            enrollmentDao.updateStatus(connection, enrollmentId, newStatus,
                    newStatus == EnrollmentStatus.WITHDRAWN ? date : null);
        });
    }

    /** Historial de matrículas de un estudiante; lanza un error si el estudiante no existe. */
    public List<Enrollment> history(int studentId) {
        return transactions.read(connection -> {
            ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            return enrollmentDao.findByStudent(connection, studentId);
        });
    }
}
