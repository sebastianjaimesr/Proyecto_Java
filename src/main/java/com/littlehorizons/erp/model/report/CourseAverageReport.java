package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Average of a subject for every student of the course (grade and optional
 * section) in a year.
 *
 * @param students      subject average of each student with grades
 * @param courseAverage mean of the student averages; {@code null} without grades
 */
public record CourseAverageReport(
        int subjectId,
        String subjectName,
        String gradeName,
        int academicYear,
        String section,
        List<StudentGrade> students,
        BigDecimal courseAverage,
        BigDecimal highest,
        BigDecimal lowest) {

    public CourseAverageReport {
        students = List.copyOf(students);
    }

    /** Average of one student in the subject. */
    public record StudentGrade(int studentId, String studentName, BigDecimal average, boolean finalGrade) {
    }
}
