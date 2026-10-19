package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.WeekDay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code asignaciones_profesor} y de sus franjas semanales en
 * {@code horarios_clase} (cambio autorizado P-1).
 */
public interface TeacherAssignmentDao {

    int insert(Connection connection, TeacherAssignment assignment) throws SQLException;

    Optional<TeacherAssignment> findById(Connection connection, int assignmentId) throws SQLException;

    /** Mismo profesor, materia, año y sección (las secciones NULL se comparan como iguales). */
    boolean exists(Connection connection, int teacherId, int subjectId, int academicYear, String section)
            throws SQLException;

    /** Si el profesor tiene alguna asignación de la materia en ese año. */
    boolean isAssigned(Connection connection, int teacherId, int subjectId, int academicYear) throws SQLException;

    List<TeacherAssignment> findByTeacher(Connection connection, int teacherId, int academicYear) throws SQLException;

    List<TeacherAssignment> findBySubject(Connection connection, int subjectId, int academicYear) throws SQLException;

    // ------------------------------------------------------- horarios_clase

    int insertSchedule(Connection connection, ClassSchedule schedule) throws SQLException;

    void deleteSchedule(Connection connection, int scheduleId) throws SQLException;

    List<ClassSchedule> findSchedulesByAssignment(Connection connection, int assignmentId) throws SQLException;

    /** Franjas de todas las asignaciones del profesor en ese año y día. */
    List<ClassSchedule> findTeacherSchedules(Connection connection, int teacherId, int academicYear, WeekDay day)
            throws SQLException;

    /** Franjas de todas las asignaciones del grado y sección en ese año y día. */
    List<ClassSchedule> findGroupSchedules(Connection connection, int gradeId, String section, int academicYear,
                                           WeekDay day) throws SQLException;

    /** Horario semanal de un profesor en ese año. */
    List<ClassSchedule> findTeacherTimetable(Connection connection, int teacherId, int academicYear)
            throws SQLException;
}
