package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * JDBC implementation of {@link EnrollmentDao}.
 */
public class JdbcEnrollmentDao implements EnrollmentDao {

    private static final String SELECT = "SELECT m.id_matricula, m.id_estudiante, m.id_grado, g.nombre_grado, "
            + "m.año_academico, m.estado_matricula, m.fecha_matricula, m.fecha_retiro, m.observaciones "
            + "FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado ";
    private static final String SQL_INSERT = "INSERT INTO matriculas (id_estudiante, id_grado, año_academico, "
            + "estado_matricula, fecha_matricula, fecha_retiro, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_STATUS =
            "UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? WHERE id_matricula = ?";

    @Override
    public int insert(Connection connection, Enrollment enrollment) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setInt(1, enrollment.studentId());
            statement.setInt(2, enrollment.gradeId());
            statement.setInt(3, enrollment.academicYear());
            setEnum(statement, 4, enrollment.status());
            setDate(statement, 5, enrollment.enrollmentDate());
            setDate(statement, 6, enrollment.withdrawalDate());
            setString(statement, 7, enrollment.notes());
        });
    }

    @Override
    public void updateStatus(Connection connection, int enrollmentId, EnrollmentStatus status,
                             LocalDate withdrawalDate) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE_STATUS, statement -> {
            setEnum(statement, 1, status);
            setDate(statement, 2, withdrawalDate);
            statement.setInt(3, enrollmentId);
        });
    }

    @Override
    public Optional<Enrollment> findById(Connection connection, int enrollmentId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE m.id_matricula = ?",
                statement -> statement.setInt(1, enrollmentId), JdbcEnrollmentDao::map);
    }

    @Override
    public Optional<Enrollment> findByStudentAndYear(Connection connection, int studentId, int academicYear)
            throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE m.id_estudiante = ? AND m.año_academico = ?",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, academicYear);
                }, JdbcEnrollmentDao::map);
    }

    @Override
    public List<Enrollment> findByStudent(Connection connection, int studentId) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE m.id_estudiante = ? ORDER BY m.año_academico DESC",
                statement -> statement.setInt(1, studentId), JdbcEnrollmentDao::map);
    }

    private static Enrollment map(ResultSet resultSet) throws SQLException {
        return new Enrollment(
                resultSet.getInt("id_matricula"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_grado"),
                resultSet.getString("nombre_grado"),
                resultSet.getInt("año_academico"),
                EnrollmentStatus.fromDbValue(resultSet.getString("estado_matricula")),
                getDate(resultSet, "fecha_matricula"),
                getDate(resultSet, "fecha_retiro"),
                resultSet.getString("observaciones"));
    }
}
