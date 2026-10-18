package com.littlehorizons.erp.model.academic;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Grade of a student in a subject and period ({@code registros_academicos}).
 * {@code value} is a DECIMAL(2,1) between 0.0 and 5.0.
 *
 * @param id          {@code null} before the record is inserted
 * @param studentName joined, for display
 * @param subjectName joined, for display
 * @param teacherName joined, for display
 */
public record GradeRecord(
        Integer id,
        int studentId,
        int subjectId,
        int teacherId,
        int academicYear,
        GradingPeriod period,
        BigDecimal value,
        String notes,
        LocalDate recordDate,
        String studentName,
        String subjectName,
        String teacherName) {
}
