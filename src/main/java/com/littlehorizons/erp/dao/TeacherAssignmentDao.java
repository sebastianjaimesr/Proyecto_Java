package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.WeekDay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code asignaciones_profesor} and their weekly slots in
 * {@code horarios_clase} (authorized change P-1).
 */
public interface TeacherAssignmentDao {

    int insert(Connection connection, TeacherAssignment assignment) throws SQLException;

    Optional<TeacherAssignment> findById(Connection connection, int assignmentId) throws SQLException;

    /** Same teacher, subject, year and section (NULL sections compare as equal). */
    boolean exists(Connection connection, int teacherId, int subjectId, int academicYear, String section)
            throws SQLException;

    /** Whether the teacher has any assignment for the subject in that year. */
    boolean isAssigned(Connection connection, int teacherId, int subjectId, int academicYear) throws SQLException;

    List<TeacherAssignment> findByTeacher(Connection connection, int teacherId, int academicYear) throws SQLException;

    List<TeacherAssignment> findBySubject(Connection connection, int subjectId, int academicYear) throws SQLException;

    // ------------------------------------------------------- horarios_clase

    int insertSchedule(Connection connection, ClassSchedule schedule) throws SQLException;

    void deleteSchedule(Connection connection, int scheduleId) throws SQLException;

    List<ClassSchedule> findSchedulesByAssignment(Connection connection, int assignmentId) throws SQLException;

    /** Slots of every assignment of the teacher in that year and day. */
    List<ClassSchedule> findTeacherSchedules(Connection connection, int teacherId, int academicYear, WeekDay day)
            throws SQLException;

    /** Slots of every assignment of the grade and section in that year and day. */
    List<ClassSchedule> findGroupSchedules(Connection connection, int gradeId, String section, int academicYear,
                                           WeekDay day) throws SQLException;

    /** Weekly timetable of a teacher in that year. */
    List<ClassSchedule> findTeacherTimetable(Connection connection, int teacherId, int academicYear)
            throws SQLException;
}
