package com.littlehorizons.erp.model.academic;

import java.time.LocalDate;

/**
 * Registration of a student in a subject for one period ({@code inscripciones_materia}).
 *
 * @param id          {@code null} before the record is inserted
 * @param subjectName joined, for display
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
