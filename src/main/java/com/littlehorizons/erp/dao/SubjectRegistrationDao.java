package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.AcademicPeriod;
import com.littlehorizons.erp.model.academic.RegistrationStatus;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.SubjectRegistration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code inscripciones_materia}.
 */
public interface SubjectRegistrationDao {

    int insert(Connection connection, SubjectRegistration registration) throws SQLException;

    void updateStatus(Connection connection, int registrationId, RegistrationStatus status) throws SQLException;

    Optional<SubjectRegistration> find(Connection connection, int studentId, int subjectId, int academicYear,
                                       AcademicPeriod period) throws SQLException;

    /** Registrations of a student in a year, ordered by subject and period. */
    List<SubjectRegistration> findByStudent(Connection connection, int studentId, int academicYear)
            throws SQLException;

    /** Whether the student has a non-withdrawn registration in the subject in any period of the year. */
    boolean hasActiveRegistration(Connection connection, int studentId, int subjectId, int academicYear)
            throws SQLException;

    /** Students with a non-withdrawn registration in the subject and year; {@code section} null means all. */
    List<Student> findStudentsBySubject(Connection connection, int subjectId, int academicYear, String section)
            throws SQLException;
}
