package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.report.EventTypeParticipation;
import com.littlehorizons.erp.model.report.GradeEnrollmentCount;
import com.littlehorizons.erp.model.report.SchoolStatistics;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-only aggregate queries for "Reportes y estadísticas". Counting and
 * summing is done by MySQL so large tables are not loaded into memory.
 */
public interface ReportDao {

    /** Every grade (Preescolar to Once) with its enrollments of the year by status. */
    List<GradeEnrollmentCount> enrollmentsByGrade(Connection connection, int academicYear) throws SQLException;

    SchoolStatistics statistics(Connection connection, int academicYear) throws SQLException;

    List<EventTypeParticipation> eventParticipationByType(Connection connection, int year) throws SQLException;

    /** Grades of every student in a year (student and subject names included). */
    List<GradeRecord> gradesOfYear(Connection connection, int academicYear) throws SQLException;
}
