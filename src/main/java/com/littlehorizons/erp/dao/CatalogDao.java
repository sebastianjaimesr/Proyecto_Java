package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Grade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Acceso de solo lectura al catálogo {@code grados}, que se carga con el
 * script oficial y la aplicación no edita (Segregación de interfaces:
 * no expone métodos de escritura).
 */
public interface CatalogDao {

    List<Grade> findAllGrades(Connection connection) throws SQLException;

    Optional<Grade> findGradeById(Connection connection, int gradeId) throws SQLException;
}
