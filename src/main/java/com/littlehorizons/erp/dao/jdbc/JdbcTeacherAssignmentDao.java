package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.model.academic.ClassSchedule;
import com.littlehorizons.erp.model.academic.TeacherAssignment;
import com.littlehorizons.erp.model.academic.WeekDay;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getTime;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setTime;

/**
 * Implementación JDBC de {@link TeacherAssignmentDao}.
 */
public class JdbcTeacherAssignmentDao implements TeacherAssignmentDao {

    private static final String SELECT = "SELECT ap.id_asignacion, ap.id_profesor, ap.id_materia, ap.año_academico, "
            + "ap.seccion, CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS nombre_profesor, "
            + "ma.nombre_materia, ma.id_grado FROM asignaciones_profesor ap "
            + "JOIN profesores p ON p.id_profesor = ap.id_profesor "
            + "JOIN materias ma ON ma.id_materia = ap.id_materia ";
    private static final String SQL_INSERT = "INSERT INTO asignaciones_profesor "
            + "(id_profesor, id_materia, año_academico, seccion) VALUES (?, ?, ?, ?)";
    private static final String SQL_EXISTS = "SELECT 1 FROM asignaciones_profesor WHERE id_profesor = ? "
            + "AND id_materia = ? AND año_academico = ? AND seccion <=> ?";
    private static final String SQL_IS_ASSIGNED = "SELECT 1 FROM asignaciones_profesor WHERE id_profesor = ? "
            + "AND id_materia = ? AND año_academico = ? LIMIT 1";

    private static final String SCHEDULE_SELECT = "SELECT h.id_horario, h.id_asignacion, h.dia_semana, "
            + "h.hora_inicio, h.hora_fin, h.aula, CONCAT(ma.nombre_materia, ' (', g.nombre_grado, "
            + "IFNULL(CONCAT(' ', ap.seccion), ''), ') - ', p.primer_nombre, ' ', p.primer_apellido) AS descripcion "
            + "FROM horarios_clase h "
            + "JOIN asignaciones_profesor ap ON ap.id_asignacion = h.id_asignacion "
            + "JOIN materias ma ON ma.id_materia = ap.id_materia "
            + "JOIN grados g ON g.id_grado = ma.id_grado "
            + "JOIN profesores p ON p.id_profesor = ap.id_profesor ";
    private static final String SCHEDULE_ORDER = " ORDER BY FIELD(h.dia_semana, 'Lunes', 'Martes', 'Miércoles', "
            + "'Jueves', 'Viernes', 'Sábado'), h.hora_inicio";
    private static final String SQL_INSERT_SCHEDULE = "INSERT INTO horarios_clase "
            + "(id_asignacion, dia_semana, hora_inicio, hora_fin, aula) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_DELETE_SCHEDULE = "DELETE FROM horarios_clase WHERE id_horario = ?";

    @Override
    public int insert(Connection connection, TeacherAssignment assignment) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setInt(1, assignment.teacherId());
            statement.setInt(2, assignment.subjectId());
            statement.setInt(3, assignment.academicYear());
            setString(statement, 4, assignment.section());
        });
    }

    @Override
    public Optional<TeacherAssignment> findById(Connection connection, int assignmentId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE ap.id_asignacion = ?",
                statement -> statement.setInt(1, assignmentId), JdbcTeacherAssignmentDao::map);
    }

    @Override
    public boolean exists(Connection connection, int teacherId, int subjectId, int academicYear, String section)
            throws SQLException {
        return JdbcSupport.exists(connection, SQL_EXISTS, statement -> {
            statement.setInt(1, teacherId);
            statement.setInt(2, subjectId);
            statement.setInt(3, academicYear);
            setString(statement, 4, section);
        });
    }

    @Override
    public boolean isAssigned(Connection connection, int teacherId, int subjectId, int academicYear)
            throws SQLException {
        return JdbcSupport.exists(connection, SQL_IS_ASSIGNED, statement -> {
            statement.setInt(1, teacherId);
            statement.setInt(2, subjectId);
            statement.setInt(3, academicYear);
        });
    }

    @Override
    public List<TeacherAssignment> findByTeacher(Connection connection, int teacherId, int academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection,
                SELECT + "WHERE ap.id_profesor = ? AND ap.año_academico = ? ORDER BY ma.nombre_materia, ap.seccion",
                statement -> {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, academicYear);
                }, JdbcTeacherAssignmentDao::map);
    }

    @Override
    public List<TeacherAssignment> findBySubject(Connection connection, int subjectId, int academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection,
                SELECT + "WHERE ap.id_materia = ? AND ap.año_academico = ? ORDER BY ap.seccion",
                statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, academicYear);
                }, JdbcTeacherAssignmentDao::map);
    }

    @Override
    public int insertSchedule(Connection connection, ClassSchedule schedule) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT_SCHEDULE, statement -> {
            statement.setInt(1, schedule.assignmentId());
            setEnum(statement, 2, schedule.day());
            setTime(statement, 3, schedule.startTime());
            setTime(statement, 4, schedule.endTime());
            setString(statement, 5, schedule.room());
        });
    }

    @Override
    public void deleteSchedule(Connection connection, int scheduleId) throws SQLException {
        JdbcSupport.update(connection, SQL_DELETE_SCHEDULE, statement -> statement.setInt(1, scheduleId));
    }

    @Override
    public List<ClassSchedule> findSchedulesByAssignment(Connection connection, int assignmentId)
            throws SQLException {
        return JdbcSupport.queryList(connection, SCHEDULE_SELECT + "WHERE h.id_asignacion = ?" + SCHEDULE_ORDER,
                statement -> statement.setInt(1, assignmentId), JdbcTeacherAssignmentDao::mapSchedule);
    }

    @Override
    public List<ClassSchedule> findTeacherSchedules(Connection connection, int teacherId, int academicYear,
                                                    WeekDay day) throws SQLException {
        return JdbcSupport.queryList(connection, SCHEDULE_SELECT
                        + "WHERE ap.id_profesor = ? AND ap.año_academico = ? AND h.dia_semana = ?" + SCHEDULE_ORDER,
                statement -> {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, academicYear);
                    setEnum(statement, 3, day);
                }, JdbcTeacherAssignmentDao::mapSchedule);
    }

    @Override
    public List<ClassSchedule> findGroupSchedules(Connection connection, int gradeId, String section,
                                                  int academicYear, WeekDay day) throws SQLException {
        return JdbcSupport.queryList(connection, SCHEDULE_SELECT
                        + "WHERE ma.id_grado = ? AND ap.seccion <=> ? AND ap.año_academico = ? AND h.dia_semana = ?"
                        + SCHEDULE_ORDER,
                statement -> {
                    statement.setInt(1, gradeId);
                    setString(statement, 2, section);
                    statement.setInt(3, academicYear);
                    setEnum(statement, 4, day);
                }, JdbcTeacherAssignmentDao::mapSchedule);
    }

    @Override
    public List<ClassSchedule> findTeacherTimetable(Connection connection, int teacherId, int academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection, SCHEDULE_SELECT
                        + "WHERE ap.id_profesor = ? AND ap.año_academico = ?" + SCHEDULE_ORDER,
                statement -> {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, academicYear);
                }, JdbcTeacherAssignmentDao::mapSchedule);
    }

    /** Convierte una fila de {@code asignaciones_profesor} en un objeto {@link TeacherAssignment}. */
    private static TeacherAssignment map(ResultSet resultSet) throws SQLException {
        return new TeacherAssignment(
                resultSet.getInt("id_asignacion"),
                resultSet.getInt("id_profesor"),
                resultSet.getInt("id_materia"),
                resultSet.getInt("año_academico"),
                resultSet.getString("seccion"),
                resultSet.getString("nombre_profesor"),
                resultSet.getString("nombre_materia"),
                resultSet.getInt("id_grado"));
    }

    /** Convierte una fila de {@code horarios_clase} en un objeto {@link ClassSchedule}. */
    private static ClassSchedule mapSchedule(ResultSet resultSet) throws SQLException {
        return new ClassSchedule(
                resultSet.getInt("id_horario"),
                resultSet.getInt("id_asignacion"),
                WeekDay.fromDbValue(resultSet.getString("dia_semana")),
                getTime(resultSet, "hora_inicio"),
                getTime(resultSet, "hora_fin"),
                resultSet.getString("aula"),
                resultSet.getString("descripcion"));
    }
}
