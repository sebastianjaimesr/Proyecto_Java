package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.report.EventTypeParticipation;
import com.littlehorizons.erp.model.report.GradeEnrollmentCount;
import com.littlehorizons.erp.model.report.SchoolStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC implementation of {@link ReportDao}.
 */
public class JdbcReportDao implements ReportDao {

    private static final String SQL_BY_GRADE = "SELECT g.nombre_grado, g.nivel_numerico, "
            + "COALESCE(SUM(m.estado_matricula = 'Activa'), 0) AS activas, "
            + "COALESCE(SUM(m.estado_matricula = 'Completada'), 0) AS completadas, "
            + "COALESCE(SUM(m.estado_matricula = 'Retirada'), 0) AS retiradas "
            + "FROM grados g LEFT JOIN matriculas m ON m.id_grado = g.id_grado AND m.año_academico = ? "
            + "GROUP BY g.id_grado, g.nombre_grado, g.nivel_numerico ORDER BY g.nivel_numerico";

    private static final String SQL_EVENTS_BY_TYPE = "SELECT ev.tipo_evento, COUNT(DISTINCT ev.id_evento) AS eventos, "
            + "COUNT(pe.id_participacion) AS participaciones, COUNT(DISTINCT pe.id_estudiante) AS estudiantes "
            + "FROM eventos ev LEFT JOIN participantes_evento pe ON pe.id_evento = ev.id_evento "
            + "WHERE YEAR(ev.fecha_evento) = ? GROUP BY ev.tipo_evento ORDER BY eventos DESC";

    private static final String SQL_GRADES_OF_YEAR = "SELECT r.id_estudiante, r.id_materia, r.id_profesor, "
            + "r.año_academico, r.periodo_academico, r.valor_nota, "
            + "CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) AS nombre_estudiante, "
            + "ma.nombre_materia FROM registros_academicos r "
            + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
            + "JOIN materias ma ON ma.id_materia = r.id_materia WHERE r.año_academico = ?";

    @Override
    public List<GradeEnrollmentCount> enrollmentsByGrade(Connection connection, int academicYear) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_BY_GRADE, statement -> statement.setInt(1, academicYear),
                resultSet -> new GradeEnrollmentCount(resultSet.getString("nombre_grado"),
                        resultSet.getInt("nivel_numerico"), resultSet.getInt("activas"),
                        resultSet.getInt("completadas"), resultSet.getInt("retiradas")));
    }

    @Override
    public SchoolStatistics statistics(Connection connection, int year) throws SQLException {
        JdbcSupport.Binder byYear = statement -> statement.setInt(1, year);
        int activeStudents = count(connection, "SELECT COUNT(*) FROM estudiantes WHERE estado = 'Activo'");
        int activeEnrollments = JdbcSupport.queryInt(connection,
                "SELECT COUNT(*) FROM matriculas WHERE año_academico = ? AND estado_matricula = 'Activa'", byYear);
        int activeTeachers = count(connection, "SELECT COUNT(*) FROM profesores WHERE estado = 'Activo'");
        int guardians = count(connection, "SELECT COUNT(*) FROM acudientes");
        int activeClubs = count(connection, "SELECT COUNT(*) FROM clubes WHERE esta_activo = TRUE");
        int members = count(connection, "SELECT COUNT(*) FROM membresias_club WHERE estado_membresia = 'Activa'");
        int events = JdbcSupport.queryInt(connection, "SELECT COUNT(*) FROM eventos WHERE YEAR(fecha_evento) = ?", byYear);
        int participations = JdbcSupport.queryInt(connection, "SELECT COUNT(*) FROM participantes_evento pe "
                + "JOIN eventos ev ON ev.id_evento = pe.id_evento WHERE YEAR(ev.fecha_evento) = ?", byYear);
        int resources = count(connection, "SELECT COUNT(*) FROM recursos");
        int loaned = count(connection, "SELECT COUNT(*) FROM recursos WHERE estado_recurso = 'Prestado'");
        int openLoans = count(connection, "SELECT (SELECT COUNT(*) FROM prestamos_estudiante WHERE estado_prestamo <> "
                + "'Devuelto') + (SELECT COUNT(*) FROM prestamos_profesor WHERE estado_prestamo <> 'Devuelto')");
        String paymentsSql = "FROM pagos WHERE COALESCE(año_academico, YEAR(fecha_pago)) = ?";
        BigDecimal paymentsTotal = JdbcSupport.queryOne(connection, "SELECT COALESCE(SUM(monto_pagado), 0) " + paymentsSql,
                byYear, resultSet -> resultSet.getBigDecimal(1)).orElse(BigDecimal.ZERO);
        int paymentsCount = JdbcSupport.queryInt(connection, "SELECT COUNT(*) " + paymentsSql, byYear);
        BigDecimal average = JdbcSupport.queryOne(connection, "SELECT AVG(valor_nota) FROM registros_academicos "
                        + "WHERE año_academico = ? AND periodo_academico <> 'Final'", byYear,
                resultSet -> resultSet.getBigDecimal(1)).orElse(null);
        int gradeRecords = JdbcSupport.queryInt(connection,
                "SELECT COUNT(*) FROM registros_academicos WHERE año_academico = ?", byYear);
        int absences = JdbcSupport.queryInt(connection, "SELECT COUNT(*) FROM asistencias "
                + "WHERE YEAR(fecha_asistencia) = ? AND estado_asistencia = 'Ausente'", byYear);
        return new SchoolStatistics(year, activeStudents, activeEnrollments, activeTeachers, guardians, activeClubs,
                members, events, participations, resources, loaned, openLoans, paymentsTotal, paymentsCount,
                average == null ? null : average.setScale(1, RoundingMode.HALF_UP), gradeRecords, absences);
    }

    @Override
    public List<EventTypeParticipation> eventParticipationByType(Connection connection, int year) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_EVENTS_BY_TYPE, statement -> statement.setInt(1, year),
                resultSet -> new EventTypeParticipation(resultSet.getString("tipo_evento"), resultSet.getInt("eventos"),
                        resultSet.getInt("participaciones"), resultSet.getInt("estudiantes")));
    }

    @Override
    public List<GradeRecord> gradesOfYear(Connection connection, int academicYear) throws SQLException {
        return JdbcSupport.queryList(connection, SQL_GRADES_OF_YEAR, statement -> statement.setInt(1, academicYear),
                resultSet -> new GradeRecord(null, resultSet.getInt("id_estudiante"), resultSet.getInt("id_materia"),
                        resultSet.getInt("id_profesor"), resultSet.getInt("año_academico"),
                        GradingPeriod.fromDbValue(resultSet.getString("periodo_academico")),
                        resultSet.getBigDecimal("valor_nota"), null, null, resultSet.getString("nombre_estudiante"),
                        resultSet.getString("nombre_materia"), null));
    }

    private static int count(Connection connection, String sql) throws SQLException {
        return JdbcSupport.queryInt(connection, sql, JdbcSupport.NO_PARAMETERS);
    }
}
