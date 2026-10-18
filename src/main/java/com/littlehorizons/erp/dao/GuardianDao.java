package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.academic.Guardian;
import com.littlehorizons.erp.model.academic.GuardianLink;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentLink;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code acudientes} and of the many-to-many relation
 * {@code estudiante_acudiente}.
 */
public interface GuardianDao {

    int insert(Connection connection, Guardian guardian) throws SQLException;

    void update(Connection connection, Guardian guardian) throws SQLException;

    Optional<Guardian> findById(Connection connection, int guardianId) throws SQLException;

    Optional<Guardian> findByDocument(Connection connection, String documentNumber) throws SQLException;

    List<Guardian> search(Connection connection, String text, int limit) throws SQLException;

    // ------------------------------------------------- estudiante_acudiente

    void link(Connection connection, int studentId, int guardianId, boolean primary, boolean emergencyContact)
            throws SQLException;

    void updateLink(Connection connection, int studentId, int guardianId, boolean primary, boolean emergencyContact)
            throws SQLException;

    /** Clears the "principal" flag of every guardian of the student. */
    void clearPrimary(Connection connection, int studentId) throws SQLException;

    boolean isLinked(Connection connection, int studentId, int guardianId) throws SQLException;

    void unlink(Connection connection, int studentId, int guardianId) throws SQLException;

    List<GuardianLink> findGuardiansOfStudent(Connection connection, int studentId) throws SQLException;

    List<StudentLink> findStudentsOfGuardian(Connection connection, int guardianId) throws SQLException;

    /** Other students that share at least one guardian with {@code studentId}. */
    List<Student> findSiblings(Connection connection, int studentId) throws SQLException;
}
