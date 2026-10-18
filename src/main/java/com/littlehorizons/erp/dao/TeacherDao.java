package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Teacher;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code profesores}. Teachers are deactivated, not deleted.
 */
public interface TeacherDao {

    int insert(Connection connection, Teacher teacher) throws SQLException;

    void update(Connection connection, Teacher teacher) throws SQLException;

    Optional<Teacher> findById(Connection connection, int teacherId) throws SQLException;

    Optional<Teacher> findByDocument(Connection connection, String documentNumber) throws SQLException;

    Optional<Teacher> findByEmail(Connection connection, String email) throws SQLException;

    List<Teacher> search(Connection connection, String text, int limit) throws SQLException;
}
