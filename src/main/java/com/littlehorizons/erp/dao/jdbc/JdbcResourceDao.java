package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceCategory;
import com.littlehorizons.erp.model.resources.ResourceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link ResourceDao}.
 */
public class JdbcResourceDao implements ResourceDao {

    private static final String SELECT = "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.id_categoria, "
            + "c.nombre_categoria, r.descripcion, r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso "
            + "FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria ";
    private static final String SQL_CATEGORIES =
            "SELECT id_categoria, nombre_categoria, descripcion FROM categorias_recurso ORDER BY nombre_categoria";
    private static final String SQL_INSERT = "INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, "
            + "descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE recursos SET codigo_recurso = ?, nombre_recurso = ?, "
            + "id_categoria = ?, descripcion = ?, fecha_adquisicion = ?, ubicacion_fisica = ?, estado_recurso = ? "
            + "WHERE id_recurso = ?";
    private static final String SQL_STATUS = "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?";
    private static final String SQL_LOCK = "SELECT id_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE";
    private static final String SQL_SEARCH = SELECT
            + "WHERE (? = '' OR r.codigo_recurso LIKE ? OR r.nombre_recurso LIKE ?) "
            + "AND (? IS NULL OR r.id_categoria = ?) AND (? IS NULL OR r.estado_recurso = ?) "
            + "ORDER BY c.nombre_categoria, r.nombre_recurso LIMIT ?";

    @Override
    public List<ResourceCategory> findCategories(Connection connection) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_CATEGORIES, JdbcSupport.NO_PARAMETERS,
                resultSet -> new ResourceCategory(resultSet.getInt("id_categoria"),
                        resultSet.getString("nombre_categoria"), resultSet.getString("descripcion")));
    }

    @Override
    public int insert(Connection connection, Resource resource) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, resource));
    }

    @Override
    public void update(Connection connection, Resource resource) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, resource);
            statement.setInt(8, resource.id());
        });
    }

    @Override
    public void updateStatus(Connection connection, int resourceId, ResourceStatus status) throws SQLException {
        JdbcSupport.update(connection, SQL_STATUS, statement -> {
            setEnum(statement, 1, status);
            statement.setInt(2, resourceId);
        });
    }

    @Override
    public Optional<Resource> findById(Connection connection, int resourceId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE r.id_recurso = ?",
                statement -> statement.setInt(1, resourceId), JdbcResourceDao::map);
    }

    @Override
    public Optional<Resource> lockById(Connection connection, int resourceId) throws SQLException {
        boolean exists = JdbcSupport.exists(connection, SQL_LOCK, statement -> statement.setInt(1, resourceId));
        return exists ? findById(connection, resourceId) : Optional.empty();
    }

    @Override
    public Optional<Resource> findByCode(Connection connection, String code) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE r.codigo_recurso = ?",
                statement -> statement.setString(1, code), JdbcResourceDao::map);
    }

    @Override
    public List<Resource> search(Connection connection, String text, Integer categoryId, ResourceStatus status,
                                 int limit) throws SQLException {
        String filter = text == null ? "" : text.strip();
        String pattern = JdbcSupport.containsPattern(filter);
        return JdbcSupport.queryList(connection, SQL_SEARCH, statement -> {
            statement.setString(1, filter);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            setInteger(statement, 4, categoryId);
            setInteger(statement, 5, categoryId);
            setEnum(statement, 6, status);
            setEnum(statement, 7, status);
            statement.setInt(8, limit);
        }, JdbcResourceDao::map);
    }

    private static void bindFields(PreparedStatement statement, Resource resource) throws SQLException {
        statement.setString(1, resource.code());
        statement.setString(2, resource.name());
        statement.setInt(3, resource.categoryId());
        setString(statement, 4, resource.description());
        setDate(statement, 5, resource.acquisitionDate());
        setString(statement, 6, resource.location());
        setEnum(statement, 7, resource.status());
    }

    private static Resource map(ResultSet resultSet) throws SQLException {
        return new Resource(
                resultSet.getInt("id_recurso"),
                resultSet.getString("codigo_recurso"),
                resultSet.getString("nombre_recurso"),
                resultSet.getInt("id_categoria"),
                resultSet.getString("nombre_categoria"),
                resultSet.getString("descripcion"),
                getDate(resultSet, "fecha_adquisicion"),
                resultSet.getString("ubicacion_fisica"),
                ResourceStatus.fromDbValue(resultSet.getString("estado_recurso")));
    }
}
