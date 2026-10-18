package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Grade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the {@code grados} catalogue, which is loaded by the
 * official script and is not edited by the application (Interface Segregation:
 * no write methods are exposed).
 */
public interface CatalogDao {

    List<Grade> findAllGrades(Connection connection) throws SQLException;

    Optional<Grade> findGradeById(Connection connection, int gradeId) throws SQLException;
}
