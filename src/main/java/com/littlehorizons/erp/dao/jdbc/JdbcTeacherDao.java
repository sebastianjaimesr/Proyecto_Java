package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Teacher;
import com.littlehorizons.erp.model.academic.TeacherStatus;
import com.littlehorizons.erp.model.common.PersonName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * JDBC implementation of {@link TeacherDao}.
 */
public class JdbcTeacherDao implements TeacherDao {

    static final String COLUMNS = "p.id_profesor, p.tipo_documento, p.numero_documento, p.primer_nombre, "
            + "p.segundo_nombre, p.primer_apellido, p.segundo_apellido, p.correo_electronico, p.telefono, "
            + "p.especializacion, p.fecha_contratacion, p.estado";

    private static final String SQL_INSERT = "INSERT INTO profesores (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, "
            + "especializacion, fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE profesores SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, "
            + "correo_electronico = ?, telefono = ?, especializacion = ?, fecha_contratacion = ?, estado = ? "
            + "WHERE id_profesor = ?";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM profesores p ";
    private static final String SQL_SEARCH = SELECT
            + "WHERE ? = '' OR p.numero_documento LIKE ? OR p.especializacion LIKE ? "
            + "OR CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) LIKE ? "
            + "ORDER BY p.primer_apellido, p.segundo_apellido, p.primer_nombre LIMIT ?";

    @Override
    public int insert(Connection connection, Teacher teacher) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, teacher));
    }

    @Override
    public void update(Connection connection, Teacher teacher) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, teacher);
            statement.setInt(12, teacher.id());
        });
    }

    @Override
    public Optional<Teacher> findById(Connection connection, int teacherId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE p.id_profesor = ?",
                statement -> statement.setInt(1, teacherId), JdbcTeacherDao::map);
    }

    @Override
    public Optional<Teacher> findByDocument(Connection connection, String documentNumber) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE p.numero_documento = ?",
                statement -> statement.setString(1, documentNumber), JdbcTeacherDao::map);
    }

    @Override
    public Optional<Teacher> findByEmail(Connection connection, String email) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE p.correo_electronico = ?",
                statement -> statement.setString(1, email), JdbcTeacherDao::map);
    }

    @Override
    public List<Teacher> search(Connection connection, String text, int limit) throws SQLException {
        String filter = text == null ? "" : text.strip();
        String pattern = JdbcSupport.containsPattern(filter);
        return JdbcSupport.queryList(connection, SQL_SEARCH, statement -> {
            statement.setString(1, filter);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setInt(5, limit);
        }, JdbcTeacherDao::map);
    }

    private static void bindFields(PreparedStatement statement, Teacher teacher) throws SQLException {
        setEnum(statement, 1, teacher.documentType());
        statement.setString(2, teacher.documentNumber());
        statement.setString(3, teacher.name().firstName());
        setString(statement, 4, teacher.name().middleName());
        statement.setString(5, teacher.name().lastName());
        setString(statement, 6, teacher.name().secondLastName());
        statement.setString(7, teacher.email());
        setString(statement, 8, teacher.phone());
        setString(statement, 9, teacher.specialization());
        setDate(statement, 10, teacher.hireDate());
        setEnum(statement, 11, teacher.status());
    }

    static Teacher map(ResultSet resultSet) throws SQLException {
        return new Teacher(
                resultSet.getInt("id_profesor"),
                PersonDocumentType.fromDbValue(resultSet.getString("tipo_documento")),
                resultSet.getString("numero_documento"),
                new PersonName(resultSet.getString("primer_nombre"), resultSet.getString("segundo_nombre"),
                        resultSet.getString("primer_apellido"), resultSet.getString("segundo_apellido")),
                resultSet.getString("correo_electronico"),
                resultSet.getString("telefono"),
                resultSet.getString("especializacion"),
                getDate(resultSet, "fecha_contratacion"),
                TeacherStatus.fromDbValue(resultSet.getString("estado")));
    }
}
