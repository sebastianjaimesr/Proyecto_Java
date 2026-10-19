package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.extracurricular.CompetitionPosition;
import com.littlehorizons.erp.model.extracurricular.Event;
import com.littlehorizons.erp.model.extracurricular.EventParticipant;
import com.littlehorizons.erp.model.extracurricular.EventType;
import com.littlehorizons.erp.model.extracurricular.ParticipationRole;
import com.littlehorizons.erp.util.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Eventos escolares, participantes y resultados de competencias.
 *
 * <p>Reglas: una participación por estudiante y evento (UNIQUE
 * participacion_unica); posiciones solo en eventos con competencia; cada
 * posición de podio (1.º, 2.º, 3.º) se otorga a un solo estudiante por evento, mientras
 * "Participante" se puede dar a muchos.</p>
 */
public class EventService {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_PLACE_LENGTH = 150;

    private final TransactionManager transactions;
    private final EventDao eventDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public EventService(TransactionManager transactions, EventDao eventDao, StudentDao studentDao,
                        TeacherDao teacherDao) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.eventDao = Objects.requireNonNull(eventDao, "eventDao");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
    }

    /** Crea un evento después de validar sus datos y su organizador. */
    public Event create(Event draft) {
        Event event = normalize(draft);
        return transactions.inTransaction(connection -> {
            checkOrganizer(connection, event.organizerId());
            int id = eventDao.insert(connection, event);
            return eventDao.findById(connection, id).orElseThrow();
        });
    }

    /** Actualiza un evento; la competencia no se puede desactivar si ya hay posiciones. */
    public Event update(Event changes) {
        Objects.requireNonNull(changes.id(), "id");
        Event event = normalize(changes);
        return transactions.inTransaction(connection -> {
            ServiceChecks.require(eventDao.findById(connection, event.id()),
                    "No existe un evento con id " + event.id() + ".");
            checkOrganizer(connection, event.organizerId());
            boolean hasPositions = eventDao.findParticipants(connection, event.id()).stream()
                    .anyMatch(participant -> participant.position() != null);
            if (!event.competition() && hasPositions) {
                throw new BusinessRuleException("El evento ya tiene posiciones registradas; no se puede quitar la competencia.");
            }
            eventDao.update(connection, event);
            return eventDao.findById(connection, event.id()).orElseThrow();
        });
    }

    /** Busca un evento por id o lanza un error si no existe. */
    public Event findById(int eventId) {
        return transactions.read(connection -> ServiceChecks.require(eventDao.findById(connection, eventId),
                "No existe un evento con id " + eventId + "."));
    }

    /** Eventos de un año; {@code type} null significa todos los tipos. */
    public List<Event> findByYear(int year, EventType type) {
        return transactions.read(connection -> eventDao.findByYear(connection, year, type));
    }

    /** Inscribe a un estudiante en un evento con un rol (la base de datos impide inscribirlo dos veces). */
    public EventParticipant addParticipant(int eventId, int studentId, ParticipationRole role, String notes) {
        if (role == null) {
            throw new ValidationException("El rol de participación es obligatorio.");
        }
        String cleanNotes = InputValidator.optionalText(notes, "observaciones", 1000);
        return transactions.inTransaction(connection -> {
            Event event = requireEvent(connection, eventId);
            if (!event.active()) {
                throw new BusinessRuleException("El evento " + event.name() + " está inactivo.");
            }
            Student student = ServiceChecks.require(studentDao.findById(connection, studentId),
                    "No existe un estudiante con id " + studentId + ".");
            if (eventDao.findParticipant(connection, eventId, studentId).isPresent()) {
                throw new BusinessRuleException(student.fullName() + " ya está registrado en " + event.name() + ".");
            }
            eventDao.insertParticipant(connection, new EventParticipant(null, eventId, event.name(), event.date(),
                    event.type(), studentId, student.fullName(), role, null, cleanNotes));
            return eventDao.findParticipant(connection, eventId, studentId).orElseThrow();
        });
    }

    /** Registra la posición que obtuvo un participante en una competencia. */
    public EventParticipant recordResult(int eventId, int studentId, CompetitionPosition position, String notes) {
        if (position == null) {
            throw new ValidationException("La posición es obligatoria.");
        }
        String cleanNotes = InputValidator.optionalText(notes, "observaciones", 1000);
        return transactions.inTransaction(connection -> {
            Event event = requireEvent(connection, eventId);
            if (!event.competition()) {
                throw new BusinessRuleException("El evento " + event.name() + " no tiene competencia.");
            }
            EventParticipant participant = ServiceChecks.require(eventDao.findParticipant(connection, eventId, studentId),
                    "El estudiante no está registrado en el evento.");
            if (position.isPodium()) {
                Optional<EventParticipant> holder = eventDao.findByPosition(connection, eventId, position);
                if (holder.isPresent() && holder.get().studentId() != studentId) {
                    throw new BusinessRuleException("El " + position.dbValue() + " ya fue asignado a "
                            + holder.get().studentName() + ".");
                }
            }
            ParticipationRole role = participant.role() == ParticipationRole.PARTICIPANT
                    ? ParticipationRole.COMPETITOR : participant.role();
            eventDao.updateParticipation(connection, participant.id(), role, position,
                    cleanNotes == null ? participant.notes() : cleanNotes);
            return eventDao.findParticipant(connection, eventId, studentId).orElseThrow();
        });
    }

    /** Participantes de un evento. */
    public List<EventParticipant> findParticipants(int eventId) {
        return transactions.read(connection -> {
            requireEvent(connection, eventId);
            return eventDao.findParticipants(connection, eventId);
        });
    }

    /** Historial de eventos de un estudiante. */
    public List<EventParticipant> findByStudent(int studentId) {
        return transactions.read(connection -> eventDao.findByStudent(connection, studentId));
    }

    /** Resultados de competencias de un año (posiciones registradas). */
    public List<EventParticipant> findResults(int year) {
        return transactions.read(connection -> eventDao.findResults(connection, year));
    }

    /** Busca el evento o lanza un error si no existe. */
    private Event requireEvent(Connection connection, int eventId) throws SQLException {
        return ServiceChecks.require(eventDao.findById(connection, eventId), "No existe un evento con id " + eventId + ".");
    }

    /** Si hay organizador, verifica que el profesor exista. */
    private void checkOrganizer(Connection connection, Integer teacherId) throws SQLException {
        if (teacherId != null) {
            ServiceChecks.require(teacherDao.findById(connection, teacherId),
                    "No existe un profesor con id " + teacherId + ".");
        }
    }

    /** Valida y limpia los datos del evento (tipo y fecha obligatorios, nombre válido). */
    private static Event normalize(Event event) {
        if (event.type() == null || event.date() == null) {
            throw new ValidationException("El tipo y la fecha del evento son obligatorios.");
        }
        return new Event(event.id(), InputValidator.requireText(event.name(), "nombre del evento", MAX_NAME_LENGTH),
                InputValidator.optionalText(event.description(), "descripción", 2000), event.type(), event.date(),
                InputValidator.optionalText(event.place(), "lugar", MAX_PLACE_LENGTH), event.competition(),
                event.organizerId(), event.organizerName(), event.active());
    }
}
