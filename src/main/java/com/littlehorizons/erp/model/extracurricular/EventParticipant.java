package com.littlehorizons.erp.model.extracurricular;

import java.time.LocalDate;

/**
 * Participación de un estudiante en un evento ({@code participantes_evento}).
 *
 * @param id          {@code null} antes de insertar el registro
 * @param eventName   obtenido con join, para mostrar
 * @param eventDate   obtenido con join, para mostrar
 * @param studentName obtenido con join, para mostrar
 * @param position    {@code null} hasta que se registre un resultado
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
