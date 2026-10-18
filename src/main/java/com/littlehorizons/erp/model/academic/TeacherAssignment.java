package com.littlehorizons.erp.model.academic;

/**
 * Teacher assigned to a subject and section for one year ({@code asignaciones_profesor}).
 *
 * @param id          {@code null} before the record is inserted
 * @param section     group such as "A"; may be {@code null}
 * @param teacherName joined, for display
 * @param subjectName joined, for display
 * @param gradeId     grade of the subject, joined
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
