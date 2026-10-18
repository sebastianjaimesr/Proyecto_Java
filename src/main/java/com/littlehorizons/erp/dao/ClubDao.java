package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code clubes} and {@code membresias_club}.
 */
public interface ClubDao {

    int insert(Connection connection, Club club) throws SQLException;

    void update(Connection connection, Club club) throws SQLException;

    Optional<Club> findById(Connection connection, int clubId) throws SQLException;

    /**
     * Reads the club with {@code SELECT ... FOR UPDATE}. Inside a transaction
     * this serializes concurrent enrollments in the same club, so two users
     * cannot take the last seat at the same time.
     */
    Optional<Club> lockById(Connection connection, int clubId) throws SQLException;

    Optional<Club> findByName(Connection connection, String name) throws SQLException;

    List<Club> findAll(Connection connection, boolean onlyActive) throws SQLException;

    int countActiveMembers(Connection connection, int clubId) throws SQLException;

    // ------------------------------------------------------- memberships

    int insertMembership(Connection connection, ClubMembership membership) throws SQLException;

    Optional<ClubMembership> findMembershipById(Connection connection, int membershipId) throws SQLException;

    Optional<ClubMembership> findActiveMembership(Connection connection, int studentId, int clubId)
            throws SQLException;

    void withdrawMembership(Connection connection, int membershipId, LocalDate date, String reason)
            throws SQLException;

    List<ClubMembership> findMembers(Connection connection, int clubId, boolean onlyActive) throws SQLException;

    /** Every membership of a student, including withdrawn ones (history). */
    List<ClubMembership> findByStudent(Connection connection, int studentId) throws SQLException;
}
