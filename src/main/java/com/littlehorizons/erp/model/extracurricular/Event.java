package com.littlehorizons.erp.model.extracurricular;

import java.time.LocalDate;

/**
 * School event ({@code eventos}).
 *
 * @param id            {@code null} before the record is inserted
 * @param competition   whether participants can obtain positions
 * @param organizerId   teacher who organizes it; may be {@code null}
 * @param organizerName joined, for display
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
