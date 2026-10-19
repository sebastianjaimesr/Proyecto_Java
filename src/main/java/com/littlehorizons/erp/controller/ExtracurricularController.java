package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;
import com.littlehorizons.erp.model.extracurricular.CompetitionPosition;
import com.littlehorizons.erp.model.extracurricular.Event;
import com.littlehorizons.erp.model.extracurricular.EventParticipant;
import com.littlehorizons.erp.model.extracurricular.EventType;
import com.littlehorizons.erp.model.extracurricular.ParticipationRole;
import com.littlehorizons.erp.service.ClubService;
import com.littlehorizons.erp.service.EventService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Coordina "Actividades extracurriculares": clubes y eventos.
 */
public class ExtracurricularController extends BaseController {

    private final ClubService clubService;
    private final EventService eventService;

    public ExtracurricularController(ClubService clubService, EventService eventService) {
        this.clubService = Objects.requireNonNull(clubService, "clubService");
        this.eventService = Objects.requireNonNull(eventService, "eventService");
    }

    public OperationResult<Club> createClub(Club draft) {
        return execute(() -> clubService.create(draft), club -> "Club creado con id " + club.id() + ".");
    }

    public OperationResult<Club> updateClub(Club changes) {
        return execute(() -> clubService.update(changes), "Club actualizado.");
    }

    public OperationResult<List<Club>> findClubs(boolean onlyActive) {
        return execute(() -> clubService.findAll(onlyActive), list -> list.size() + " club(es).");
    }

    public OperationResult<ClubMembership> enrollInClub(int studentId, int clubId, LocalDate date) {
        return execute(() -> clubService.enroll(studentId, clubId, date),
                membership -> "Estudiante inscrito en " + membership.clubName() + ".");
    }

    public OperationResult<Boolean> withdrawFromClub(int membershipId, LocalDate date, String reason) {
        return execute(() -> {
            clubService.withdraw(membershipId, date, reason);
            return Boolean.TRUE;
        }, "Retiro del club registrado. La membresía queda en el historial.");
    }

    public OperationResult<List<ClubMembership>> findMembers(int clubId, boolean onlyActive) {
        return execute(() -> clubService.findMembers(clubId, onlyActive), list -> list.size() + " miembro(s).");
    }

    public OperationResult<List<ClubMembership>> findClubsOfStudent(int studentId) {
        return execute(() -> clubService.findByStudent(studentId), list -> list.size() + " membresía(s).");
    }

    public OperationResult<Event> createEvent(Event draft) {
        return execute(() -> eventService.create(draft), event -> "Evento creado con id " + event.id() + ".");
    }

    public OperationResult<Event> updateEvent(Event changes) {
        return execute(() -> eventService.update(changes), "Evento actualizado.");
    }

    public OperationResult<List<Event>> findEvents(int year, EventType type) {
        return execute(() -> eventService.findByYear(year, type), list -> list.size() + " evento(s).");
    }

    public OperationResult<EventParticipant> addParticipant(int eventId, int studentId, ParticipationRole role,
                                                            String notes) {
        return execute(() -> eventService.addParticipant(eventId, studentId, role, notes),
                participant -> participant.studentName() + " registrado en " + participant.eventName() + ".");
    }

    public OperationResult<EventParticipant> recordResult(int eventId, int studentId, CompetitionPosition position,
                                                          String notes) {
        return execute(() -> eventService.recordResult(eventId, studentId, position, notes),
                participant -> "Resultado registrado: " + participant.studentName() + " - "
                        + participant.position().dbValue() + ".");
    }

    public OperationResult<List<EventParticipant>> findParticipants(int eventId) {
        return execute(() -> eventService.findParticipants(eventId), list -> list.size() + " participante(s).");
    }

    public OperationResult<List<EventParticipant>> findEventsOfStudent(int studentId) {
        return execute(() -> eventService.findByStudent(studentId), list -> list.size() + " participación(es).");
    }

    public OperationResult<List<EventParticipant>> findResults(int year) {
        return execute(() -> eventService.findResults(year), list -> list.size() + " resultado(s).");
    }
}
