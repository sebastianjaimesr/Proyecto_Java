package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.SubjectRegistrationDao;
import com.littlehorizons.erp.model.academic.AcademicPeriod;
import com.littlehorizons.erp.model.academic.RegistrationStatus;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.SubjectRegistration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link SubjectRegistrationDao}.
 */
public class JdbcSubjectRegistrationDao implements SubjectRegistrationDao {

    private static final String SELECT = "SELECT i.id_inscripcion, i.id_estudiante, i.id_materia, ma.nombre_materia, "
            + "i.año_academico, i.periodo_academico, i.seccion, i.fecha_inscripcion, i.estado "
            + "FROM inscripciones_materia i JOIN materias ma ON ma.id_materia = i.id_materia ";
    private static final String SQL_INSERT = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, "
            + "año_academico, periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_STATUS = "UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?";
    private static final String SQL_HAS_ACTIVE = "SELECT 1 FROM inscripciones_materia WHERE id_estudiante = ? "
            + "AND id_materia = ? AND año_academico = ? AND estado <> 'Retirada' LIMIT 1";

    private static final String SQL_STUDENTS_BY_SUBJECT = "SELECT DISTINCT " + JdbcStudentDao.COLUMNS
            + " FROM inscripciones_materia i JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
            + "WHERE i.id_materia = ? AND i.año_academico = ? AND i.estado <> 'Retirada' "
            + "AND (? IS NULL OR i.seccion = ?) ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";

    @Override
    public int insert(Connection connection, SubjectRegistration registration) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setInt(1, registration.studentId());
            statement.setInt(2, registration.subjectId());
            statement.setInt(3, registration.academicYear());
            setEnum(statement, 4, registration.period());
            setString(statement, 5, registration.section());
            setDate(statement, 6, registration.registrationDate());
            setEnum(statement, 7, registration.status());
        });
    }

    @Override
    public void updateStatus(Connection connection, int registrationId, RegistrationStatus status)
            throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE_STATUS, statement -> {
            setEnum(statement, 1, status);
            statement.setInt(2, registrationId);
        });
    }

    @Override
    public Optional<SubjectRegistration> find(Connection connection, int studentId, int subjectId, int academicYear,
                                              AcademicPeriod period) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE i.id_estudiante = ? AND i.id_materia = ? "
                + "AND i.año_academico = ? AND i.periodo_academico = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    setEnum(statement, 4, period);
                }, JdbcSubjectRegistrationDao::map);
    }

    @Override
    public List<SubjectRegistration> findByStudent(Connection connection, int studentId, int academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE i.id_estudiante = ? AND i.año_academico = ? "
                + "ORDER BY ma.nombre_materia, i.periodo_academico", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, academicYear);
                }, JdbcSubjectRegistrationDao::map);
    }

    @Override
    public boolean hasActiveRegistration(Connection connection, int studentId, int subjectId, int academicYear)
            throws SQLException {
        return JdbcSupport.exists(connection, SQL_HAS_ACTIVE, statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, subjectId);
            statement.setInt(3, academicYear);
        });
    }

    @Override
    public List<Student> findStudentsBySubject(Connection connection, int subjectId, int academicYear, String section)
            throws SQLException {
        return JdbcSupport.queryList(connection, SQL_STUDENTS_BY_SUBJECT, statement -> {
            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
            setString(statement, 3, section);
            setString(statement, 4, section);
        }, JdbcStudentDao::map);
    }

    /** Convierte una fila de {@code inscripciones_materia} en un objeto {@link SubjectRegistration}. */
    private static SubjectRegistration map(ResultSet resultSet) throws SQLException {
        return new SubjectRegistration(
                resultSet.getInt("id_inscripcion"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_materia"),
                resultSet.getString("nombre_materia"),
                resultSet.getInt("año_academico"),
                AcademicPeriod.fromDbValue(resultSet.getString("periodo_academico")),
                resultSet.getString("seccion"),
                getDate(resultSet, "fecha_inscripcion"),
                RegistrationStatus.fromDbValue(resultSet.getString("estado")));
    }
}
