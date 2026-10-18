package com.littlehorizons.erp.model.extracurricular;

/**
 * Extracurricular club ({@code clubes}).
 *
 * @param id             {@code null} before the record is inserted
 * @param supervisorId   teacher in charge; may be {@code null}
 * @param supervisorName joined, for display
 * @param capacity       maximum number of active members
 * @param activeMembers  computed count of active memberships, for display
 */
public record Club(
        Integer id,
        String name,
        String description,
        Integer supervisorId,
        String supervisorName,
        int capacity,
        String meetingSchedule,
        boolean active,
        int activeMembers) {

    /** Default of {@code clubes.capacidad_maxima} in the official schema. */
    public static final int DEFAULT_CAPACITY = 30;

    public int availableSeats() {
        return Math.max(0, capacity - activeMembers);
    }
}
