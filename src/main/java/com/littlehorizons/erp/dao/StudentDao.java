package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code estudiantes}. Students are never deleted: withdrawing
 * a student changes its status, because related tables cascade on delete.
 */
public interface StudentDao {

    int insert(Connection connection, Student student) throws SQLException;

    void update(Connection connection, Student student) throws SQLException;

    void updateStatus(Connection connection, int studentId, StudentStatus status) throws SQLException;

    Optional<Student> findById(Connection connection, int studentId) throws SQLException;

    Optional<Student> findByDocument(Connection connection, String documentNumber) throws SQLException;

    /** Students whose name or document contains {@code text}; all when it is blank. */
    List<Student> search(Connection connection, String text, int limit) throws SQLException;
}
