package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Subject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code materias}. Subjects are deactivated, not deleted.
 */
public interface SubjectDao {

    int insert(Connection connection, Subject subject) throws SQLException;

    void update(Connection connection, Subject subject) throws SQLException;

    Optional<Subject> findById(Connection connection, int subjectId) throws SQLException;

    Optional<Subject> findByCode(Connection connection, String code) throws SQLException;

    /** Subjects of a grade; {@code onlyActive} filters out deactivated ones. */
    List<Subject> findByGrade(Connection connection, int gradeId, boolean onlyActive) throws SQLException;

    List<Subject> findAll(Connection connection) throws SQLException;
}
