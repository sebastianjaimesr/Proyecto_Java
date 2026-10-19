package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.GradeRecordDao;
import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDecimal;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * Implementación JDBC de {@link GradeRecordDao}.
 */
public class JdbcGradeRecordDao implements GradeRecordDao {

    private static final String SELECT = "SELECT r.id_registro, r.id_estudiante, r.id_materia, r.id_profesor, "
            + "r.año_academico, r.periodo_academico, r.valor_nota, r.observaciones, r.fecha_registro, "
            + "CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) AS nombre_estudiante, "
            + "ma.nombre_materia, CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS nombre_profesor "
            + "FROM registros_academicos r "
            + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
            + "JOIN materias ma ON ma.id_materia = r.id_materia "
            + "JOIN profesores p ON p.id_profesor = r.id_profesor ";
    private static final String SQL_INSERT = "INSERT INTO registros_academicos (id_estudiante, id_materia, "
            + "id_profesor, año_academico, periodo_academico, valor_nota, observaciones, fecha_registro) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_VALUE = "UPDATE registros_academicos SET id_profesor = ?, valor_nota = ?, "
            + "observaciones = ?, fecha_registro = ? WHERE id_registro = ?";
    private static final String SQL_BY_SUBJECT = SELECT + "WHERE r.id_materia = ? AND r.año_academico = ? "
            + "AND (? IS NULL OR EXISTS (SELECT 1 FROM inscripciones_materia i WHERE i.id_estudiante = r.id_estudiante "
            + "AND i.id_materia = r.id_materia AND i.año_academico = r.año_academico AND i.seccion = ?)) "
            + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre, r.periodo_academico";

    @Override
    public int insert(Connection connection, GradeRecord gradeRecord) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> {
            statement.setInt(1, gradeRecord.studentId());
            statement.setInt(2, gradeRecord.subjectId());
            statement.setInt(3, gradeRecord.teacherId());
            statement.setInt(4, gradeRecord.academicYear());
            setEnum(statement, 5, gradeRecord.period());
            setDecimal(statement, 6, gradeRecord.value());
            setString(statement, 7, gradeRecord.notes());
            setDate(statement, 8, gradeRecord.recordDate());
        });
    }

    @Override
    public void updateValue(Connection connection, int recordId, int teacherId, BigDecimal value, String notes,
                            LocalDate recordDate) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE_VALUE, statement -> {
            statement.setInt(1, teacherId);
            setDecimal(statement, 2, value);
            setString(statement, 3, notes);
            setDate(statement, 4, recordDate);
            statement.setInt(5, recordId);
        });
    }

    @Override
    public Optional<GradeRecord> find(Connection connection, int studentId, int subjectId, int academicYear,
                                      GradingPeriod period) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE r.id_estudiante = ? AND r.id_materia = ? "
                + "AND r.año_academico = ? AND r.periodo_academico = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    setEnum(statement, 4, period);
                }, JdbcGradeRecordDao::map);
    }

    @Override
    public List<GradeRecord> findByStudent(Connection connection, int studentId, Integer academicYear)
            throws SQLException {
        return JdbcSupport.queryList(connection, SELECT + "WHERE r.id_estudiante = ? "
                + "AND (? IS NULL OR r.año_academico = ?) "
                + "ORDER BY r.año_academico, ma.nombre_materia, r.periodo_academico", statement -> {
                    statement.setInt(1, studentId);
                    setInteger(statement, 2, academicYear);
                    setInteger(statement, 3, academicYear);
                }, JdbcGradeRecordDao::map);
    }

    @Override
    public List<GradeRecord> findBySubject(Connection connection, int subjectId, int academicYear, String section)
            throws SQLException {
        return JdbcSupport.queryList(connection, SQL_BY_SUBJECT, statement -> {
            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
            setString(statement, 3, section);
            setString(statement, 4, section);
        }, JdbcGradeRecordDao::map);
    }

    private static GradeRecord map(ResultSet resultSet) throws SQLException {
        return new GradeRecord(
                resultSet.getInt("id_registro"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_materia"),
                resultSet.getInt("id_profesor"),
                resultSet.getInt("año_academico"),
                GradingPeriod.fromDbValue(resultSet.getString("periodo_academico")),
                resultSet.getBigDecimal("valor_nota"),
                resultSet.getString("observaciones"),
                getDate(resultSet, "fecha_registro"),
                resultSet.getString("nombre_estudiante"),
                resultSet.getString("nombre_materia"),
                resultSet.getString("nombre_profesor"));
    }
}
