package test.dao;

import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.common.PersonName;
import test.model.TeacherSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcTeacherSummaryDao implements TeacherSummaryDao {

    private static final String SQL_FIND_BY_ID = "SELECT id_profesor, tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido "
            + "FROM profesores WHERE id_profesor = ?";

    @Override
    public Optional<TeacherSummary> findById(Connection connection, int teacherId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL_FIND_BY_ID)) {
            statement.setInt(1, teacherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private static TeacherSummary map(ResultSet resultSet) throws SQLException {
        PersonName name = new PersonName(
                resultSet.getString("primer_nombre"),
                resultSet.getString("segundo_nombre"),
                resultSet.getString("primer_apellido"),
                resultSet.getString("segundo_apellido"));
        return new TeacherSummary(
                resultSet.getInt("id_profesor"),
                documentType(resultSet.getString("tipo_documento")),
                resultSet.getString("numero_documento"),
                name);
    }

    private static PersonDocumentType documentType(String value) throws SQLException {
        try {
            return PersonDocumentType.fromDbValue(value);
        } catch (IllegalArgumentException e) {
            throw new SQLDataException("Unsupported tipo_documento value: " + value, e);
        }
    }
}
