package com.littlehorizons.erp.model.academic;

/**
 * Profesor asignado a una materia y sección durante un año ({@code asignaciones_profesor}).
 *
 * @param id          {@code null} antes de insertar el registro
 * @param section     grupo, por ejemplo "A"; puede ser {@code null}
 * @param teacherName obtenido con join, para mostrar
 * @param subjectName obtenido con join, para mostrar
 * @param gradeId     grado de la materia, obtenido con join
 */
public record TeacherAssignment(
        Integer id,
        int teacherId,
        int subjectId,
        int academicYear,
        String section,
        String teacherName,
        String subjectName,
        int gradeId) {
}
