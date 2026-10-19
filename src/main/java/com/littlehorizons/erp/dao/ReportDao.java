package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.report.EventTypeParticipation;
import com.littlehorizons.erp.model.report.GradeEnrollmentCount;
import com.littlehorizons.erp.model.report.SchoolStatistics;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Consultas agregadas de solo lectura para "Reportes y estadísticas". Los conteos y
 * sumas los hace MySQL, así las tablas grandes no se cargan en memoria.
 */
public interface ReportDao {

    /** Cada grado (Preescolar a Once) con sus matrículas del año por estado. */
    List<GradeEnrollmentCount> enrollmentsByGrade(Connection connection, int academicYear) throws SQLException;

    SchoolStatistics statistics(Connection connection, int academicYear) throws SQLException;

    List<EventTypeParticipation> eventParticipationByType(Connection connection, int year) throws SQLException;

    /** Notas de todos los estudiantes en un año (incluye nombres de estudiante y materia). */
    List<GradeRecord> gradesOfYear(Connection connection, int academicYear) throws SQLException;
}
