package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.model.extracurricular.CompetitionPosition;
import com.littlehorizons.erp.model.extracurricular.Event;
import com.littlehorizons.erp.model.extracurricular.EventParticipant;
import com.littlehorizons.erp.model.extracurricular.EventType;
import com.littlehorizons.erp.model.extracurricular.ParticipationRole;
import com.littlehorizons.erp.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.getInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setDate;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setEnum;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setInteger;
import static com.littlehorizons.erp.dao.jdbc.JdbcSupport.setString;

/**
 * JDBC implementation of {@link EventDao}.
 */
public class JdbcEventDao implements EventDao {

    private static final String SELECT = "SELECT ev.id_evento, ev.nombre_evento, ev.descripcion, ev.tipo_evento, "
            + "ev.fecha_evento, ev.lugar, ev.tiene_competencia, ev.id_organizador, "
            + "CONCAT_WS(' ', p.primer_nombre, p.primer_apellido) AS nombre_organizador, ev.esta_activo "
            + "FROM eventos ev LEFT JOIN profesores p ON p.id_profesor = ev.id_organizador ";
    private static final String SQL_INSERT = "INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, "
            + "fecha_evento, lugar, tiene_competencia, id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, "
            + "fecha_evento = ?, lugar = ?, tiene_competencia = ?, id_organizador = ?, esta_activo = ? "
            + "WHERE id_evento = ?";

    private static final String PARTICIPANT_SELECT = "SELECT pe.id_participacion, pe.id_evento, ev.nombre_evento, "
            + "ev.fecha_evento, ev.tipo_evento, pe.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) AS nombre_estudiante, "
            + "pe.rol_participacion, pe.posicion_obtenida, pe.observaciones "
            + "FROM participantes_evento pe JOIN eventos ev ON ev.id_evento = pe.id_evento "
            + "JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante ";
    private static final String POSITION_ORDER = "FIELD(pe.posicion_obtenida, '1er Lugar', '2do Lugar', "
            + "'3er Lugar', 'Participante') ";
    private static final String SQL_INSERT_PARTICIPANT = "INSERT INTO participantes_evento (id_evento, id_estudiante, "
            + "rol_participacion, posicion_obtenida, observaciones) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_PARTICIPATION = "UPDATE participantes_evento SET rol_participacion = ?, "
            + "posicion_obtenida = ?, observaciones = ? WHERE id_participacion = ?";

    @Override
    public int insert(Connection connection, Event event) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT, statement -> bindFields(statement, event));
    }

    @Override
    public void update(Connection connection, Event event) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE, statement -> {
            bindFields(statement, event);
            statement.setInt(9, event.id());
        });
    }

    @Override
    public Optional<Event> findById(Connection connection, int eventId) throws SQLException {
        return JdbcSupport.queryOne(connection, SELECT + "WHERE ev.id_evento = ?",
                statement -> statement.setInt(1, eventId), JdbcEventDao::map);
    }

    @Override
    public List<Event> findByYear(Connection connection, int year, EventType type) throws SQLException {
        return JdbcSupport.queryList(connection, SELECT
                + "WHERE YEAR(ev.fecha_evento) = ? AND (? IS NULL OR ev.tipo_evento = ?) ORDER BY ev.fecha_evento",
                statement -> {
                    statement.setInt(1, year);
                    setEnum(statement, 2, type);
                    setEnum(statement, 3, type);
                }, JdbcEventDao::map);
    }

    @Override
    public int insertParticipant(Connection connection, EventParticipant participant) throws SQLException {
        return JdbcSupport.insert(connection, SQL_INSERT_PARTICIPANT, statement -> {
            statement.setInt(1, participant.eventId());
            statement.setInt(2, participant.studentId());
            setEnum(statement, 3, participant.role());
            setEnum(statement, 4, participant.position());
            setString(statement, 5, participant.notes());
        });
    }

    @Override
    public Optional<EventParticipant> findParticipant(Connection connection, int eventId, int studentId)
            throws SQLException {
        return JdbcSupport.queryOne(connection, PARTICIPANT_SELECT + "WHERE pe.id_evento = ? AND pe.id_estudiante = ?",
                statement -> {
                    statement.setInt(1, eventId);
                    statement.setInt(2, studentId);
                }, JdbcEventDao::mapParticipant);
    }

    @Override
    public void updateParticipation(Connection connection, int participationId, ParticipationRole role,
                                    CompetitionPosition position, String notes) throws SQLException {
        JdbcSupport.update(connection, SQL_UPDATE_PARTICIPATION, statement -> {
            setEnum(statement, 1, role);
            setEnum(statement, 2, position);
            setString(statement, 3, notes);
            statement.setInt(4, participationId);
        });
    }

    @Override
    public Optional<EventParticipant> findByPosition(Connection connection, int eventId, CompetitionPosition position)
            throws SQLException {
        return JdbcSupport.queryOne(connection, PARTICIPANT_SELECT
                + "WHERE pe.id_evento = ? AND pe.posicion_obtenida = ? LIMIT 1", statement -> {
                    statement.setInt(1, eventId);
                    setEnum(statement, 2, position);
                }, JdbcEventDao::mapParticipant);
    }

    @Override
    public List<EventParticipant> findParticipants(Connection connection, int eventId) throws SQLException {
        return JdbcSupport.queryList(connection, PARTICIPANT_SELECT + "WHERE pe.id_evento = ? ORDER BY "
                        + "pe.posicion_obtenida IS NULL, " + POSITION_ORDER + ", nombre_estudiante",
                statement -> statement.setInt(1, eventId), JdbcEventDao::mapParticipant);
    }

    @Override
    public List<EventParticipant> findByStudent(Connection connection, int studentId) throws SQLException {
        return JdbcSupport.queryList(connection, PARTICIPANT_SELECT
                        + "WHERE pe.id_estudiante = ? ORDER BY ev.fecha_evento DESC",
                statement -> statement.setInt(1, studentId), JdbcEventDao::mapParticipant);
    }

    @Override
    public List<EventParticipant> findResults(Connection connection, int year) throws SQLException {
        return JdbcSupport.queryList(connection, PARTICIPANT_SELECT
                        + "WHERE YEAR(ev.fecha_evento) = ? AND ev.tiene_competencia = TRUE "
                        + "AND pe.posicion_obtenida IS NOT NULL ORDER BY ev.fecha_evento, " + POSITION_ORDER,
                statement -> statement.setInt(1, year), JdbcEventDao::mapParticipant);
    }

    private static void bindFields(PreparedStatement statement, Event event) throws SQLException {
        statement.setString(1, event.name());
        setString(statement, 2, event.description());
        setEnum(statement, 3, event.type());
        setDate(statement, 4, event.date());
        setString(statement, 5, event.place());
        statement.setBoolean(6, event.competition());
        setInteger(statement, 7, event.organizerId());
        statement.setBoolean(8, event.active());
    }

    private static Event map(ResultSet resultSet) throws SQLException {
        return new Event(
                resultSet.getInt("id_evento"),
                resultSet.getString("nombre_evento"),
                resultSet.getString("descripcion"),
                EventType.fromDbValue(resultSet.getString("tipo_evento")),
                getDate(resultSet, "fecha_evento"),
                resultSet.getString("lugar"),
                resultSet.getBoolean("tiene_competencia"),
                getInteger(resultSet, "id_organizador"),
                resultSet.getString("nombre_organizador"),
                resultSet.getBoolean("esta_activo"));
    }

    private static EventParticipant mapParticipant(ResultSet resultSet) throws SQLException {
        return new EventParticipant(
                resultSet.getInt("id_participacion"),
                resultSet.getInt("id_evento"),
                resultSet.getString("nombre_evento"),
                getDate(resultSet, "fecha_evento"),
                EventType.fromDbValue(resultSet.getString("tipo_evento")),
                resultSet.getInt("id_estudiante"),
                resultSet.getString("nombre_estudiante"),
                ParticipationRole.fromDbValue(resultSet.getString("rol_participacion")),
                parsePosition(resultSet.getString("posicion_obtenida")),
                resultSet.getString("observaciones"));
    }

    /** The column is free text: values written outside the application are shown as "no position". */
    private static CompetitionPosition parsePosition(String value) {
        try {
            return CompetitionPosition.fromDbValue(value);
        } catch (IllegalArgumentException e) {
            AppLogger.getLogger(JdbcEventDao.class).warning("Unknown posicion_obtenida value ignored: " + value);
            return null;
        }
    }
}
