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
 * Persistencia de {@code registros_academicos}.
 */
public interface GradeRecordDao {

    int insert(Connection connection, GradeRecord gradeRecord) throws SQLException;

    /** Corrige el valor de una nota existente (mismo estudiante, materia, año y período). */
    void updateValue(Connection connection, int recordId, int teacherId, BigDecimal value, String notes,
                     LocalDate recordDate) throws SQLException;

    Optional<GradeRecord> find(Connection connection, int studentId, int subjectId, int academicYear,
                               GradingPeriod period) throws SQLException;

    /** Todas las notas de un estudiante; {@code academicYear} null significa todos los años (historial académico). */
    List<GradeRecord> findByStudent(Connection connection, int studentId, Integer academicYear) throws SQLException;

    /** Notas de una materia en un año; {@code section} null significa todas las secciones. */
    List<GradeRecord> findBySubject(Connection connection, int subjectId, int academicYear, String section)
            throws SQLException;
}
