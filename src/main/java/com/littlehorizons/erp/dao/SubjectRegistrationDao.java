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
 * Persistencia de {@code inscripciones_materia}.
 */
public interface SubjectRegistrationDao {

    int insert(Connection connection, SubjectRegistration registration) throws SQLException;

    void updateStatus(Connection connection, int registrationId, RegistrationStatus status) throws SQLException;

    Optional<SubjectRegistration> find(Connection connection, int studentId, int subjectId, int academicYear,
                                       AcademicPeriod period) throws SQLException;

    /** Inscripciones de un estudiante en un año, ordenadas por materia y período. */
    List<SubjectRegistration> findByStudent(Connection connection, int studentId, int academicYear)
            throws SQLException;

    /** Si el estudiante tiene una inscripción no retirada en la materia, en cualquier período del año. */
    boolean hasActiveRegistration(Connection connection, int studentId, int subjectId, int academicYear)
            throws SQLException;

    /** Estudiantes con inscripción no retirada en la materia y el año; {@code section} null significa todas. */
    List<Student> findStudentsBySubject(Connection connection, int subjectId, int academicYear, String section)
            throws SQLException;
}
