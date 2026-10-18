package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.model.academic.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * JDBC implementation of {@link SubjectDao}.
 */
public class JdbcSubjectDao implements SubjectDao {

    private static final String SELECT = "SELECT ma.id_materia, ma.codigo_materia, ma.nombre_materia, "
            + "ma.descripcion, ma.id_grado, g.nombre_grado, ma.horas_semanales, ma.esta_activa "
            + "FROM materias ma JOIN grados g ON g.id_grado = ma.id_grado ";
    private static final String ORDER = " ORDER BY g.nivel_numerico, ma.nombre_materia";
    private static final String SQL_INSERT = "INSERT INTO materias (codigo_materia, nombre_materia, descripcion, "
            + "id_grado, horas_semanales, esta_activa) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE materias SET codigo_materia = ?, nombre_materia = ?, "
            + "descripcion = ?, id_grado = ?, horas_semanales = ?, esta_activa = ? WHERE id_materia = ?";

    @Override
    public int insert(Connection connection, Subject subject) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, subject));
    }

    @Override
    public void update(Connection connection, Subject subject) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, subject);
            statement.setInt(7, subject.id());
        });
    }

    @Override
    public Optional<Subject> findById(Connection connection, int subjectId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE ma.id_materia = ?",
                statement -> statement.setInt(1, subjectId), JdbcSubjectDao::map);
    }

    @Override
    public Optional<Subject> findByCode(Connection connection, String code) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE ma.codigo_materia = ?",
                statement -> statement.setString(1, code), JdbcSubjectDao::map);
    }

    @Override
    public List<Subject> findByGrade(Connection connection, int gradeId, boolean onlyActive) throws SQLException {
        return JdbcSupport.queryList(connection,
                SELECT + "WHERE ma.id_grado = ? AND (? = FALSE OR ma.esta_activa = TRUE)" + ORDER,
                statement -> {
                    statement.setInt(1, gradeId);
                    statement.setBoolean(2, onlyActive);
                }, JdbcSubjectDao::map);
    }

    @Override
    public List<Subject> findAll(Connection connection) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + ORDER, JdbcSupport.NO_PARAMETERS, JdbcSubjectDao::map);
    }

    private static void bindFields(PreparedStatement statement, Subject subject) throws SQLException {
        statement.setString(1, subject.code());
        statement.setString(2, subject.name());
        setString(statement, 3, subject.description());
        statement.setInt(4, subject.gradeId());
        statement.setInt(5, subject.weeklyHours());
        statement.setBoolean(6, subject.active());
    }

    static Subject map(ResultSet resultSet) throws SQLException {
        return new Subject(
                resultSet.getInt("id_materia"),
                resultSet.getString("codigo_materia"),
                resultSet.getString("nombre_materia"),
                resultSet.getString("descripcion"),
                resultSet.getInt("id_grado"),
                resultSet.getString("nombre_grado"),
                resultSet.getInt("horas_semanales"),
                resultSet.getBoolean("esta_activa"));
    }
}
