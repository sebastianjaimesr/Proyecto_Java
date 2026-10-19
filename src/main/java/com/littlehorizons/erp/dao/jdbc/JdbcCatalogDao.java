package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.CatalogDao;
import com.littlehorizons.erp.model.academic.Grade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link CatalogDao}.
 */
public class JdbcCatalogDao implements CatalogDao {

    private static final String SQL_ALL =
            "SELECT id_grado, nombre_grado, nivel_numerico FROM grados ORDER BY nivel_numerico";
    private static final String SQL_BY_ID =
            "SELECT id_grado, nombre_grado, nivel_numerico FROM grados WHERE id_grado = ?";

    @Override
    public List<Grade> findAllGrades(Connection connection) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_ALL, JdbcSupport.NO_PARAMETERS, JdbcCatalogDao::map);
    }

    @Override
    public Optional<Grade> findGradeById(Connection connection, int gradeId) throws SQLException {
        return JdbcSupport.queryOne(connection, SQL_BY_ID, statement -> statement.setInt(1, gradeId),
                JdbcCatalogDao::map);
    }

    private static Grade map(ResultSet resultSet) throws SQLException {
        return new Grade(resultSet.getInt("id_grado"), resultSet.getString("nombre_grado"),
                resultSet.getInt("nivel_numerico"));
    }
}
