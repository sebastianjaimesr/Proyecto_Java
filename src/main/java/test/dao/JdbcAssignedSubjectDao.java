package test.dao;

import test.model.AssignedSubject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcAssignedSubjectDao implements AssignedSubjectDao {

    private static final String SQL_FIND_BY_TEACHER_AND_YEAR = "SELECT ma.codigo_materia, ma.nombre_materia, "
            + "g.nombre_grado FROM asignaciones_profesor ap "
            + "JOIN materias ma ON ma.id_materia = ap.id_materia "
            + "JOIN grados g ON g.id_grado = ma.id_grado "
            + "WHERE ap.id_profesor = ? AND ap.año_academico = ? "
            + "GROUP BY ma.id_materia, ma.codigo_materia, ma.nombre_materia, g.nombre_grado "
            + "ORDER BY MIN(ap.id_asignacion)";

    @Override
    public List<AssignedSubject> findByTeacherAndYear(Connection connection, int teacherId, int academicYear)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL_FIND_BY_TEACHER_AND_YEAR)) {
            statement.setInt(1, teacherId);
            statement.setInt(2, academicYear);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AssignedSubject> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(new AssignedSubject(
                            resultSet.getString("codigo_materia"),
                            resultSet.getString("nombre_materia"),
                            resultSet.getString("nombre_grado")));
                }
                return List.copyOf(subjects);
            }
        }
    }
}
