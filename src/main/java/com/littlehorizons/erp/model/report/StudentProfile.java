package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.Student;

import java.util.List;

/**
 * Pantalla de detalle de un estudiante: datos personales, acudientes (los contactos de emergencia son
 * acudientes marcados como tales), historial de matrículas y hermanos (estudiantes que
 * comparten un acudiente).
 */
public record StudentProfile(
        Student student,
        List<GuardianLink> guardians,
        List<Enrollment> enrollments,
        List<Student> siblings) {

    public StudentProfile {
        guardians = List.copyOf(guardians);
        enrollments = List.copyOf(enrollments);
        siblings = List.copyOf(siblings);
    }

    /** Acudientes marcados como contacto de emergencia. */
    public List<GuardianLink> emergencyContacts() {
        return guardians.stream().filter(GuardianLink::emergencyContact).toList();
    }
}
