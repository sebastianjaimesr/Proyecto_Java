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
 * Persistence of {@code asistencias}.
 */
public interface AttendanceDao {

    int insert(Connection connection, Attendance attendance) throws SQLException;

    void updateStatus(Connection connection, int attendanceId, AttendanceStatus status, String notes,
                      Integer recordedBy) throws SQLException;

    Optional<Attendance> find(Connection connection, int studentId, int subjectId, LocalDate date)
            throws SQLException;

    /** Attendance of a student between two dates (inclusive), newest first. */
    List<Attendance> findByStudent(Connection connection, int studentId, LocalDate from, LocalDate to)
            throws SQLException;

    /**
     * Students with more than {@code maxAbsences} records in status "Ausente"
     * between two dates (inclusive), with their late and excused counts.
     */
    List<AbsenceReportRow> findStudentsOverAbsenceLimit(Connection connection, LocalDate from, LocalDate to,
                                                        int maxAbsences) throws SQLException;

    /** Absence counts per student in a range, for every student with any record (reports). */
    List<AbsenceReportRow> summarizeByStudent(Connection connection, LocalDate from, LocalDate to)
            throws SQLException;
}
