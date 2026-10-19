package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.model.academic.Attendance;
import com.littlehorizons.erp.model.academic.AttendanceStatus;
import com.littlehorizons.erp.model.report.AbsenceReportRow;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link AttendanceDao}.
 */
public class JdbcAttendanceDao implements AttendanceDao {

    private static final String SELECT = "SELECT a.id_asistencia, a.id_estudiante, a.id_materia, a.fecha_asistencia, "
            + "a.estado_asistencia, a.observaciones, a.registrado_por, ma.nombre_materia "
            + "FROM asistencias a JOIN materias ma ON ma.id_materia = a.id_materia ";
    private static final String SQL_INSERT = "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, "
            + "estado_asistencia, observaciones, registrado_por) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, "
            + "registrado_por = ? WHERE id_asistencia = ?";

    /**
     * Conteos por estudiante en el rango. El grado actual sale de la
     * matrícula del año de la fecha de inicio, cuando existe.
     */
    private static final String SUMMARY = "SELECT e.id_estudiante, e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) AS nombre_estudiante, "
            + "g.nombre_grado, "
            + "SUM(a.estado_asistencia = 'Ausente') AS ausencias, "
            + "SUM(a.estado_asistencia = 'Tardanza') AS tardanzas, "
            + "SUM(a.estado_asistencia = 'Excusa Médica') AS excusas, COUNT(*) AS registros "
            + "FROM asistencias a "
            + "JOIN estudiantes e ON e.id_estudiante = a.id_estudiante "
            + "LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = YEAR(?) "
            + "LEFT JOIN grados g ON g.id_grado = m.id_grado "
            + "WHERE a.fecha_asistencia BETWEEN ? AND ? "
            + "GROUP BY e.id_estudiante, e.numero_documento, nombre_estudiante, g.nombre_grado ";

    @Override
    public int insert(Connection connection, Attendance attendance) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setInt(1, attendance.studentId());
            statement.setInt(2, attendance.subjectId());
            setDate(statement, 3, attendance.date());
            setEnum(statement, 4, attendance.status());
            setString(statement, 5, attendance.notes());
            setInteger(statement, 6, attendance.recordedBy());
        });
    }

    @Override
    public void updateStatus(Connection connection, int attendanceId, AttendanceStatus status, String notes,
                             Integer recordedBy) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            setEnum(statement, 1, status);
            setString(statement, 2, notes);
            setInteger(statement, 3, recordedBy);
            statement.setInt(4, attendanceId);
        });
    }

    @Override
    public Optional<Attendance> find(Connection connection, int studentId, int subjectId, LocalDate date)
            throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT
                + "WHERE a.id_estudiante = ? AND a.id_materia = ? AND a.fecha_asistencia = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    setDate(statement, 3, date);
                }, JdbcAttendanceDao::map);
    }

    @Override
    public List<Attendance> findByStudent(Connection connection, int studentId, LocalDate from, LocalDate to)
            throws SQLException {
        return JdbcSupport.queryList(connection, SELECT
                + "WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ? "
                + "ORDER BY a.fecha_asistencia DESC, ma.nombre_materia", statement -> {
                    statement.setInt(1, studentId);
                    setDate(statement, 2, from);
                    setDate(statement, 3, to);
                }, JdbcAttendanceDao::map);
    }

    @Override
    public List<AbsenceReportRow> findStudentsOverAbsenceLimit(Connection connection, LocalDate from, LocalDate to,
                                                               int maxAbsences) throws SQLException {
        return JdbcSupport.queryList(connection, SUMMARY + "HAVING ausencias > ? ORDER BY ausencias DESC, nombre_estudiante",
                statement -> {
                    setDate(statement, 1, from);
                    setDate(statement, 2, from);
                    setDate(statement, 3, to);
                    statement.setInt(4, maxAbsences);
                }, JdbcAttendanceDao::mapSummary);
    }

    @Override
    public List<AbsenceReportRow> summarizeByStudent(Connection connection, LocalDate from, LocalDate to)
            throws SQLException {
        return JdbcSupport.queryList(connection, SUMMARY + "ORDER BY ausencias DESC, nombre_estudiante",
                statement -> {
                    setDate(statement, 1, from);
                    setDate(statement, 2, from);
                    setDate(statement, 3, to);
                }, JdbcAttendanceDao::mapSummary);
    }

    private static Attendance map(ResultSet resultSet) throws SQLException {
        return new Attendance(
                resultSet.getInt("id_asistencia"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_materia"),
                getDate(resultSet, "fecha_asistencia"),
                AttendanceStatus.fromDbValue(resultSet.getString("estado_asistencia")),
                resultSet.getString("observaciones"),
                getInteger(resultSet, "registrado_por"),
                resultSet.getString("nombre_materia"));
    }

    private static AbsenceReportRow mapSummary(ResultSet resultSet) throws SQLException {
        return new AbsenceReportRow(
                resultSet.getInt("id_estudiante"),
                resultSet.getString("numero_documento"),
                resultSet.getString("nombre_estudiante"),
                resultSet.getString("nombre_grado"),
                resultSet.getInt("ausencias"),
                resultSet.getInt("tardanzas"),
                resultSet.getInt("excusas"),
                resultSet.getInt("registros"));
    }
}
