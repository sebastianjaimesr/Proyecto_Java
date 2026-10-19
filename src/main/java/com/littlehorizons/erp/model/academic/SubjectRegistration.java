package com.littlehorizons.erp.model.academic;

import java.time.LocalDate;

/**
 * Inscripción de un estudiante a una materia en un período ({@code inscripciones_materia}).
 *
 * @param id          {@code null} antes de insertar el registro
 * @param subjectName obtenido con join, para mostrar
 */
public record SubjectRegistration(
        Integer id,
        int studentId,
        int subjectId,
        String subjectName,
        int academicYear,
        AcademicPeriod period,
        String section,
        LocalDate registrationDate,
        RegistrationStatus status) {
}
