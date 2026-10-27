package test.dao;

import test.model.AssignedSubject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@FunctionalInterface
public interface AssignedSubjectDao {

    List<AssignedSubject> findByTeacherAndYear(Connection connection, int teacherId, int academicYear)
            throws SQLException;
}
