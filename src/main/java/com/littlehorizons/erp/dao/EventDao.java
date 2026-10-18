package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.extracurricular.CompetitionPosition;
import com.littlehorizons.erp.model.extracurricular.Event;
import com.littlehorizons.erp.model.extracurricular.EventParticipant;
import com.littlehorizons.erp.model.extracurricular.EventType;
import com.littlehorizons.erp.model.extracurricular.ParticipationRole;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code eventos} and {@code participantes_evento}.
 */
public interface EventDao {

    int insert(Connection connection, Event event) throws SQLException;

    void update(Connection connection, Event event) throws SQLException;

    Optional<Event> findById(Connection connection, int eventId) throws SQLException;

    /** Events of a year, optionally of one type, ordered by date. */
    List<Event> findByYear(Connection connection, int year, EventType type) throws SQLException;

    // ------------------------------------------------------- participants

    int insertParticipant(Connection connection, EventParticipant participant) throws SQLException;

    Optional<EventParticipant> findParticipant(Connection connection, int eventId, int studentId) throws SQLException;

    void updateParticipation(Connection connection, int participationId, ParticipationRole role,
                             CompetitionPosition position, String notes) throws SQLException;

    /** Participant holding a podium position in the event, if any. */
    Optional<EventParticipant> findByPosition(Connection connection, int eventId, CompetitionPosition position)
            throws SQLException;

    List<EventParticipant> findParticipants(Connection connection, int eventId) throws SQLException;

    /** Participation history of a student, newest event first. */
    List<EventParticipant> findByStudent(Connection connection, int studentId) throws SQLException;

    /** Every recorded position in competitions of a year (competition results report). */
    List<EventParticipant> findResults(Connection connection, int year) throws SQLException;
}
