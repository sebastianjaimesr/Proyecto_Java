package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Enrollment;
import com.littlehorizons.erp.model.academic.EnrollmentStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code matriculas} (one enrollment per student and year).
 */
public interface EnrollmentDao {

    int insert(Connection connection, Enrollment enrollment) throws SQLException;

    void updateStatus(Connection connection, int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate)
            throws SQLException;

    Optional<Enrollment> findById(Connection connection, int enrollmentId) throws SQLException;

    Optional<Enrollment> findByStudentAndYear(Connection connection, int studentId, int academicYear)
            throws SQLException;

    /** Enrollment history of a student, newest year first. */
    List<Enrollment> findByStudent(Connection connection, int studentId) throws SQLException;
}
