package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code matriculas} (una matrícula por estudiante y año).
 */
public interface EnrollmentDao {

    int insert(Connection connection, Enrollment enrollment) throws SQLException;

    void updateStatus(Connection connection, int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate)
            throws SQLException;

    Optional<Enrollment> findById(Connection connection, int enrollmentId) throws SQLException;

    Optional<Enrollment> findByStudentAndYear(Connection connection, int studentId, int academicYear)
            throws SQLException;

    /** Historial de matrículas de un estudiante, del año más reciente al más antiguo. */
    List<Enrollment> findByStudent(Connection connection, int studentId) throws SQLException;
}
