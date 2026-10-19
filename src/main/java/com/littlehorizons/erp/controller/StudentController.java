package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;
import com.littlehorizons.erp.model.academic.Grade;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentLink;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.report.StudentProfile;
import com.littlehorizons.erp.service.EnrollmentService;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.StudentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Coordina el menú "Gestión de estudiantes y acudientes" con los servicios
 * de estudiantes, acudientes y matrículas.
 *
 * <p>Patrón de todos sus métodos: llaman al servicio correspondiente dentro de
 * {@code execute(...)} (heredado de {@link BaseController}), que devuelve un
 * {@link OperationResult} con el dato y un mensaje en español; si el servicio lanza
 * una excepción, la convierte en un mensaje de error para la vista.</p>
 */
public class StudentController extends BaseController {

    private final StudentService studentService;
    private final GuardianService guardianService;
    private final EnrollmentService enrollmentService;

    public StudentController(StudentService studentService, GuardianService guardianService,
                             EnrollmentService enrollmentService) {
        this.studentService = Objects.requireNonNull(studentService, "studentService");
        this.guardianService = Objects.requireNonNull(guardianService, "guardianService");
        this.enrollmentService = Objects.requireNonNull(enrollmentService, "enrollmentService");
    }

    // --------------------------------------------------------------- estudiantes

    public OperationResult<Student> registerStudent(Student draft) {
        return execute(() -> studentService.register(draft),
                student -> "Estudiante registrado con id " + student.id() + ".");
    }

    public OperationResult<Student> updateStudent(Student changes) {
        return execute(() -> studentService.update(changes), "Datos del estudiante actualizados.");
    }

    public OperationResult<Student> withdrawStudent(int studentId, LocalDate date) {
        return execute(() -> studentService.withdraw(studentId, date),
                "Estudiante retirado. Su historial académico y financiero se conserva.");
    }

    public OperationResult<Student> changeStudentStatus(int studentId, StudentStatus status) {
        return execute(() -> studentService.changeStatus(studentId, status),
                "Estado actualizado a " + status.dbValue() + ".");
    }

    public OperationResult<List<Student>> searchStudents(String text) {
        return execute(() -> studentService.search(text), list -> list.size() + " estudiante(s) encontrado(s).");
    }

    public OperationResult<StudentProfile> getStudentProfile(int studentId) {
        return execute(() -> studentService.getProfile(studentId), "Ficha cargada.");
    }

    // -------------------------------------------------------------- acudientes

    public OperationResult<Guardian> registerGuardian(Guardian draft) {
        return execute(() -> guardianService.register(draft),
                guardian -> "Acudiente registrado con id " + guardian.id() + ".");
    }

    public OperationResult<Guardian> registerGuardianForStudent(Guardian draft, int studentId, boolean primary,
                                                                boolean emergencyContact) {
        return execute(() -> guardianService.registerForStudent(draft, studentId, primary, emergencyContact),
                guardian -> "Acudiente registrado con id " + guardian.id() + " y relacionado con el estudiante.");
    }

    public OperationResult<Guardian> updateGuardian(Guardian changes) {
        return execute(() -> guardianService.update(changes), "Datos del acudiente actualizados.");
    }

    public OperationResult<Guardian> findGuardian(int guardianId) {
        return execute(() -> guardianService.findById(guardianId), "Acudiente encontrado.");
    }

    public OperationResult<List<Guardian>> searchGuardians(String text) {
        return execute(() -> guardianService.search(text), list -> list.size() + " acudiente(s) encontrado(s).");
    }

    public OperationResult<Boolean> linkGuardian(int studentId, int guardianId, boolean primary,
                                                 boolean emergencyContact) {
        return execute(() -> {
            guardianService.link(studentId, guardianId, primary, emergencyContact);
            return Boolean.TRUE;
        }, "Relación estudiante-acudiente guardada.");
    }

    public OperationResult<Boolean> unlinkGuardian(int studentId, int guardianId) {
        return execute(() -> {
            guardianService.unlink(studentId, guardianId);
            return Boolean.TRUE;
        }, "Relación eliminada.");
    }

    public OperationResult<List<GuardianLink>> findGuardiansOfStudent(int studentId) {
        return execute(() -> guardianService.findGuardiansOfStudent(studentId),
                list -> list.size() + " acudiente(s).");
    }

    public OperationResult<List<StudentLink>> findStudentsOfGuardian(int guardianId) {
        return execute(() -> guardianService.findStudentsOfGuardian(guardianId),
                list -> list.size() + " estudiante(s) a cargo.");
    }

    // ------------------------------------------------------------ matrículas

    public OperationResult<List<Grade>> findGrades() {
        return execute(enrollmentService::findGrades, "Grados cargados.");
    }

    public OperationResult<Enrollment> enroll(int studentId, int gradeId, int academicYear, LocalDate date,
                                              String notes) {
        return execute(() -> enrollmentService.enroll(studentId, gradeId, academicYear, date, notes),
                enrollment -> "Matrícula " + enrollment.id() + " registrada en " + enrollment.gradeName()
                        + " para " + enrollment.academicYear() + ".");
    }

    public OperationResult<Boolean> closeEnrollment(int enrollmentId, EnrollmentStatus status, LocalDate date) {
        return execute(() -> {
            enrollmentService.close(enrollmentId, status, date);
            return Boolean.TRUE;
        }, "Matrícula actualizada a " + status.dbValue() + ".");
    }

    public OperationResult<List<Enrollment>> enrollmentHistory(int studentId) {
        return execute(() -> enrollmentService.history(studentId), list -> list.size() + " matrícula(s).");
    }
}
