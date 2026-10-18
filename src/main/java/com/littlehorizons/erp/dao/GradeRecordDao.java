package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code registros_academicos}.
 */
public interface GradeRecordDao {

    int insert(Connection connection, GradeRecord gradeRecord) throws SQLException;

    /** Corrects the value of an existing grade (same student, subject, year and period). */
    void updateValue(Connection connection, int recordId, int teacherId, BigDecimal value, String notes,
                     LocalDate recordDate) throws SQLException;

    Optional<GradeRecord> find(Connection connection, int studentId, int subjectId, int academicYear,
                               GradingPeriod period) throws SQLException;

    /** All grades of a student; {@code academicYear} null means every year (academic history). */
    List<GradeRecord> findByStudent(Connection connection, int studentId, Integer academicYear) throws SQLException;

    /** Grades of a subject in a year; {@code section} null means every section. */
    List<GradeRecord> findBySubject(Connection connection, int subjectId, int academicYear, String section)
            throws SQLException;
}
