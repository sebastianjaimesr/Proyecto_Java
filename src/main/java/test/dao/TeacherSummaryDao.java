package test.dao;

import test.model.TeacherSummary;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

@FunctionalInterface
public interface TeacherSummaryDao {

    Optional<TeacherSummary> findById(Connection connection, int teacherId) throws SQLException;
}
