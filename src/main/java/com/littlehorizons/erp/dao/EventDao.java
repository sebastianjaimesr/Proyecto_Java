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
 * Persistencia de {@code eventos} y {@code participantes_evento}.
 */
public interface EventDao {

    int insert(Connection connection, Event event) throws SQLException;

    void update(Connection connection, Event event) throws SQLException;

    Optional<Event> findById(Connection connection, int eventId) throws SQLException;

    /** Eventos de un año, opcionalmente de un solo tipo, ordenados por fecha. */
    List<Event> findByYear(Connection connection, int year, EventType type) throws SQLException;

    // ------------------------------------------------------- participantes

    int insertParticipant(Connection connection, EventParticipant participant) throws SQLException;

    Optional<EventParticipant> findParticipant(Connection connection, int eventId, int studentId) throws SQLException;

    void updateParticipation(Connection connection, int participationId, ParticipationRole role,
                             CompetitionPosition position, String notes) throws SQLException;

    /** Participante que ocupa una posición de podio en el evento, si lo hay. */
    Optional<EventParticipant> findByPosition(Connection connection, int eventId, CompetitionPosition position)
            throws SQLException;

    List<EventParticipant> findParticipants(Connection connection, int eventId) throws SQLException;

    /** Historial de participación de un estudiante, del evento más reciente al más antiguo. */
    List<EventParticipant> findByStudent(Connection connection, int studentId) throws SQLException;

    /** Todas las posiciones registradas en competencias de un año (reporte de resultados). */
    List<EventParticipant> findResults(Connection connection, int year) throws SQLException;
}
