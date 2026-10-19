package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentStatus;
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
 * Implementación JDBC de {@link StudentDao}.
 */
public class JdbcStudentDao implements StudentDao {

    static final String COLUMNS = "e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, "
            + "e.segundo_nombre, e.primer_apellido, e.segundo_apellido, e.fecha_nacimiento, e.genero, "
            + "e.direccion, e.telefono, e.correo_electronico, e.tipo_sangre, e.alergias, "
            + "e.condiciones_medicas, e.fecha_ingreso, e.estado";

    private static final String SQL_INSERT = "INSERT INTO estudiantes (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, "
            + "direccion, telefono, correo_electronico, tipo_sangre, alergias, condiciones_medicas, "
            + "fecha_ingreso, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, "
            + "fecha_nacimiento = ?, genero = ?, direccion = ?, telefono = ?, correo_electronico = ?, "
            + "tipo_sangre = ?, alergias = ?, condiciones_medicas = ?, fecha_ingreso = ?, estado = ? "
            + "WHERE id_estudiante = ?";
    private static final String SQL_UPDATE_STATUS = "UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?";
    private static final String SQL_BY_ID = "SELECT " + COLUMNS + " FROM estudiantes e WHERE e.id_estudiante = ?";
    private static final String SQL_BY_DOCUMENT =
            "SELECT " + COLUMNS + " FROM estudiantes e WHERE e.numero_documento = ?";
    private static final String SQL_SEARCH = "SELECT " + COLUMNS + " FROM estudiantes e "
            + "WHERE ? = '' OR e.numero_documento LIKE ? "
            + "OR CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) LIKE ? "
            + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre LIMIT ?";

    @Override
    public int insert(Connection connection, Student student) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, student));
    }

    @Override
    public void update(Connection connection, Student student) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, student);
            statement.setInt(17, student.id());
        });
    }

    @Override
    public void updateStatus(Connection connection, int studentId, StudentStatus status) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE_STATUS, statement -> {
            setEnum(statement, 1, status);
            statement.setInt(2, studentId);
        });
    }

    @Override
    public Optional<Student> findById(Connection connection, int studentId) throws SQLException {
        return JdbcSupport.queryOne(connection, SQL_BY_ID, statement -> statement.setInt(1, studentId),
                JdbcStudentDao::map);
    }

    @Override
    public Optional<Student> findByDocument(Connection connection, String documentNumber) throws SQLException {
        return JdbcSupport.queryOne(connection, SQL_BY_DOCUMENT, statement -> statement.setString(1, documentNumber),
                JdbcStudentDao::map);
    }

    @Override
    public List<Student> search(Connection connection, String text, int limit) throws SQLException {
        String filter = text == null ? "" : text.strip();
        String pattern = JdbcSupport.containsPattern(filter);
        return JdbcSupport.queryList(connection, SQL_SEARCH, statement -> {
            statement.setString(1, filter);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setInt(4, limit);
        }, JdbcStudentDao::map);
    }

    private static void bindFields(PreparedStatement statement, Student student) throws SQLException {
        setEnum(statement, 1, student.documentType());
        statement.setString(2, student.documentNumber());
        statement.setString(3, student.name().firstName());
        setString(statement, 4, student.name().middleName());
        statement.setString(5, student.name().lastName());
        setString(statement, 6, student.name().secondLastName());
        setDate(statement, 7, student.birthDate());
        setEnum(statement, 8, student.gender());
        setString(statement, 9, student.address());
        setString(statement, 10, student.phone());
        setString(statement, 11, student.email());
        setString(statement, 12, student.bloodType());
        setString(statement, 13, student.allergies());
        setString(statement, 14, student.medicalConditions());
        setDate(statement, 15, student.admissionDate());
        setEnum(statement, 16, student.status());
    }

    /** Convierte en objeto las columnas listadas en {@link #COLUMNS}; otros DAOs lo reutilizan en sus joins. */
    static Student map(ResultSet resultSet) throws SQLException {
        return new Student(
                resultSet.getInt("id_estudiante"),
                StudentDocumentType.fromDbValue(resultSet.getString("tipo_documento")),
                resultSet.getString("numero_documento"),
                new PersonName(resultSet.getString("primer_nombre"), resultSet.getString("segundo_nombre"),
                        resultSet.getString("primer_apellido"), resultSet.getString("segundo_apellido")),
                getDate(resultSet, "fecha_nacimiento"),
                Gender.fromDbValue(resultSet.getString("genero")),
                resultSet.getString("direccion"),
                resultSet.getString("telefono"),
                resultSet.getString("correo_electronico"),
                resultSet.getString("tipo_sangre"),
                resultSet.getString("alergias"),
                resultSet.getString("condiciones_medicas"),
                getDate(resultSet, "fecha_ingreso"),
                StudentStatus.fromDbValue(resultSet.getString("estado")));
    }
}
