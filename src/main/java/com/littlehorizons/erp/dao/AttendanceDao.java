package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Attendance;
import com.littlehorizons.erp.model.academic.AttendanceStatus;
import com.littlehorizons.erp.model.report.AbsenceReportRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code asistencias}.
 */
public interface AttendanceDao {

    int insert(Connection connection, Attendance attendance) throws SQLException;

    void updateStatus(Connection connection, int attendanceId, AttendanceStatus status, String notes,
                      Integer recordedBy) throws SQLException;

    Optional<Attendance> find(Connection connection, int studentId, int subjectId, LocalDate date)
            throws SQLException;

    /** Asistencia de un estudiante entre dos fechas (inclusive), de la más reciente a la más antigua. */
    List<Attendance> findByStudent(Connection connection, int studentId, LocalDate from, LocalDate to)
            throws SQLException;

    /**
     * Estudiantes con más de {@code maxAbsences} registros en estado "Ausente"
     * entre dos fechas (inclusive), con sus conteos de tardanzas y excusas.
     */
    List<AbsenceReportRow> findStudentsOverAbsenceLimit(Connection connection, LocalDate from, LocalDate to,
                                                        int maxAbsences) throws SQLException;

    /** Conteo de ausencias por estudiante en un rango, para todo estudiante con algún registro (reportes). */
    List<AbsenceReportRow> summarizeByStudent(Connection connection, LocalDate from, LocalDate to)
            throws SQLException;
}
