package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentLink;
import com.littlehorizons.erp.model.common.PersonName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link GuardianDao}.
 */
public class JdbcGuardianDao implements GuardianDao {

    static final String COLUMNS = "a.id_acudiente, a.tipo_documento, a.numero_documento, a.primer_nombre, "
            + "a.segundo_nombre, a.primer_apellido, a.segundo_apellido, a.parentesco, a.telefono_principal, "
            + "a.telefono_secundario, a.correo_electronico, a.direccion, a.ocupacion, a.es_responsable_financiero";

    private static final String SQL_INSERT = "INSERT INTO acudientes (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, "
            + "telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, parentesco = ?, "
            + "telefono_principal = ?, telefono_secundario = ?, correo_electronico = ?, direccion = ?, "
            + "ocupacion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?";
    private static final String SQL_BY_ID = "SELECT " + COLUMNS + " FROM acudientes a WHERE a.id_acudiente = ?";
    private static final String SQL_BY_DOCUMENT =
            "SELECT " + COLUMNS + " FROM acudientes a WHERE a.numero_documento = ?";
    private static final String SQL_SEARCH = "SELECT " + COLUMNS + " FROM acudientes a "
            + "WHERE ? = '' OR a.numero_documento LIKE ? "
            + "OR CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido, a.segundo_apellido) LIKE ? "
            + "ORDER BY a.primer_apellido, a.segundo_apellido, a.primer_nombre LIMIT ?";

    private static final String SQL_LINK = "INSERT INTO estudiante_acudiente "
            + "(id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE_LINK = "UPDATE estudiante_acudiente SET es_principal = ?, "
            + "es_contacto_emergencia = ? WHERE id_estudiante = ? AND id_acudiente = ?";
    private static final String SQL_CLEAR_PRIMARY =
            "UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?";
    private static final String SQL_IS_LINKED =
            "SELECT 1 FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?";
    private static final String SQL_UNLINK =
            "DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?";
    private static final String SQL_GUARDIANS_OF_STUDENT = "SELECT " + COLUMNS
            + ", ea.es_principal, ea.es_contacto_emergencia FROM estudiante_acudiente ea "
            + "JOIN acudientes a ON a.id_acudiente = ea.id_acudiente WHERE ea.id_estudiante = ? "
            + "ORDER BY ea.es_principal DESC, a.primer_apellido";
    private static final String SQL_STUDENTS_OF_GUARDIAN = "SELECT " + JdbcStudentDao.COLUMNS
            + ", ea.es_principal, ea.es_contacto_emergencia FROM estudiante_acudiente ea "
            + "JOIN estudiantes e ON e.id_estudiante = ea.id_estudiante WHERE ea.id_acudiente = ? "
            + "ORDER BY e.primer_apellido, e.primer_nombre";
    private static final String SQL_SIBLINGS = "SELECT DISTINCT " + JdbcStudentDao.COLUMNS
            + " FROM estudiante_acudiente mine "
            + "JOIN estudiante_acudiente other ON other.id_acudiente = mine.id_acudiente "
            + "AND other.id_estudiante <> mine.id_estudiante "
            + "JOIN estudiantes e ON e.id_estudiante = other.id_estudiante "
            + "WHERE mine.id_estudiante = ? ORDER BY e.primer_apellido, e.primer_nombre";

    @Override
    public int insert(Connection connection, Guardian guardian) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, guardian));
    }

    @Override
    public void update(Connection connection, Guardian guardian) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, guardian);
            statement.setInt(14, guardian.id());
        });
    }

    @Override
    public Optional<Guardian> findById(Connection connection, int guardianId) throws SQLException {
        return JdbcSupport.queryOne(connection, SQL_BY_ID, statement -> statement.setInt(1, guardianId),
                JdbcGuardianDao::map);
    }

    @Override
    public Optional<Guardian> findByDocument(Connection connection, String documentNumber) throws SQLException {
        return JdbcSupport.queryOne(connection, SQL_BY_DOCUMENT, statement -> statement.setString(1, documentNumber),
                JdbcGuardianDao::map);
    }

    @Override
    public List<Guardian> search(Connection connection, String text, int limit) throws SQLException {
        String filter = text == null ? "" : text.strip();
        String pattern = JdbcSupport.containsPattern(filter);
        return JdbcSupport.queryList(connection, SQL_SEARCH, statement -> {
            statement.setString(1, filter);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setInt(4, limit);
        }, JdbcGuardianDao::map);
    }

    @Override
    public void link(Connection connection, int studentId, int guardianId, boolean primary, boolean emergencyContact)
            throws SQLException {
        JdbcSupport.update(connection, SQL_LINK, statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, guardianId);
            statement.setBoolean(3, primary);
            statement.setBoolean(4, emergencyContact);
        });
    }

    @Override
    public void updateLink(Connection connection, int studentId, int guardianId, boolean primary,
                           boolean emergencyContact) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE_LINK, statement -> {
            statement.setBoolean(1, primary);
            statement.setBoolean(2, emergencyContact);
            statement.setInt(3, studentId);
            statement.setInt(4, guardianId);
        });
    }

    @Override
    public void clearPrimary(Connection connection, int studentId) throws SQLException {
        JdbcSupport.update(connection, SQL_CLEAR_PRIMARY, statement -> statement.setInt(1, studentId));
    }

    @Override
    public boolean isLinked(Connection connection, int studentId, int guardianId) throws SQLException {
        return JdbcSupport.exists(connection, SQL_IS_LINKED, statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, guardianId);
        });
    }

    @Override
    public void unlink(Connection connection, int studentId, int guardianId) throws SQLException {
        JdbcSupport.update(connection, SQL_UNLINK, statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, guardianId);
        });
    }

    @Override
    public List<GuardianLink> findGuardiansOfStudent(Connection connection, int studentId) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_GUARDIANS_OF_STUDENT, statement -> statement.setInt(1, studentId),
                resultSet -> new GuardianLink(map(resultSet), resultSet.getBoolean("es_principal"),
                        resultSet.getBoolean("es_contacto_emergencia")));
    }

    @Override
    public List<StudentLink> findStudentsOfGuardian(Connection connection, int guardianId) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_STUDENTS_OF_GUARDIAN,
                statement -> statement.setInt(1, guardianId),
                resultSet -> new StudentLink(JdbcStudentDao.map(resultSet), resultSet.getBoolean("es_principal"),
                        resultSet.getBoolean("es_contacto_emergencia")));
    }

    @Override
    public List<Student> findSiblings(Connection connection, int studentId) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_SIBLINGS, statement -> statement.setInt(1, studentId),
                JdbcStudentDao::map);
    }

    private static void bindFields(PreparedStatement statement, Guardian guardian) throws SQLException {
        setEnum(statement, 1, guardian.documentType());
        statement.setString(2, guardian.documentNumber());
        statement.setString(3, guardian.name().firstName());
        setString(statement, 4, guardian.name().middleName());
        statement.setString(5, guardian.name().lastName());
        setString(statement, 6, guardian.name().secondLastName());
        statement.setString(7, guardian.relationship());
        statement.setString(8, guardian.mainPhone());
        setString(statement, 9, guardian.secondaryPhone());
        setString(statement, 10, guardian.email());
        setString(statement, 11, guardian.address());
        setString(statement, 12, guardian.occupation());
        statement.setBoolean(13, guardian.financialResponsible());
    }

    /** Convierte en objeto las columnas listadas en {@link #COLUMNS}. */
    static Guardian map(ResultSet resultSet) throws SQLException {
        return new Guardian(
                resultSet.getInt("id_acudiente"),
                PersonDocumentType.fromDbValue(resultSet.getString("tipo_documento")),
                resultSet.getString("numero_documento"),
                new PersonName(resultSet.getString("primer_nombre"), resultSet.getString("segundo_nombre"),
                        resultSet.getString("primer_apellido"), resultSet.getString("segundo_apellido")),
                resultSet.getString("parentesco"),
                resultSet.getString("telefono_principal"),
                resultSet.getString("telefono_secundario"),
                resultSet.getString("correo_electronico"),
                resultSet.getString("direccion"),
                resultSet.getString("ocupacion"),
                resultSet.getBoolean("es_responsable_financiero"));
    }
}
