package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code estudiantes}. Los estudiantes nunca se borran: retirar
 * a un estudiante cambia su estado, porque las tablas relacionadas se borran en cascada.
 */
public interface StudentDao {

    int insert(Connection connection, Student student) throws SQLException;

    void update(Connection connection, Student student) throws SQLException;

    void updateStatus(Connection connection, int studentId, StudentStatus status) throws SQLException;

    Optional<Student> findById(Connection connection, int studentId) throws SQLException;

    Optional<Student> findByDocument(Connection connection, String documentNumber) throws SQLException;

    /** Estudiantes cuyo nombre o documento contiene {@code text}; todos si está vacío. */
    List<Student> search(Connection connection, String text, int limit) throws SQLException;
}
