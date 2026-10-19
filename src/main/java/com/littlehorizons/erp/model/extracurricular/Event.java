package com.littlehorizons.erp.model.extracurricular;

import java.time.LocalDate;

/**
 * Evento escolar ({@code eventos}).
 *
 * @param id            {@code null} antes de insertar el registro
 * @param competition   si los participantes pueden obtener posiciones
 * @param organizerId   profesor que lo organiza; puede ser {@code null}
 * @param organizerName obtenido con join, para mostrar
 */
public record Event(
        Integer id,
        String name,
        String description,
        EventType type,
        LocalDate date,
        String place,
        boolean competition,
        Integer organizerId,
        String organizerName,
        boolean active) {

    public Event withId(int newId) {
        return new Event(newId, name, description, type, date, place, competition, organizerId, organizerName, active);
    }
}
