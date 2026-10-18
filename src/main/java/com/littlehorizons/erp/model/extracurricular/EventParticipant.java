package com.littlehorizons.erp.model.extracurricular;

import java.time.LocalDate;

/**
 * Participation of a student in an event ({@code participantes_evento}).
 *
 * @param id          {@code null} before the record is inserted
 * @param eventName   joined, for display
 * @param eventDate   joined, for display
 * @param studentName joined, for display
 * @param position    {@code null} until a result is recorded
 */
public record EventParticipant(
        Integer id,
        int eventId,
        String eventName,
        LocalDate eventDate,
        EventType eventType,
        int studentId,
        String studentName,
        ParticipationRole role,
        CompetitionPosition position,
        String notes) {
}
