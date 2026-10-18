package com.littlehorizons.erp.model.academic;

import java.time.LocalTime;

/**
 * One weekly class slot of an assignment ({@code horarios_clase}, authorized change P-1).
 *
 * @param id          {@code null} before the record is inserted
 * @param description joined text "subject - section - teacher", for display
 */
public record ClassSchedule(
        Integer id,
        int assignmentId,
        WeekDay day,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        String description) {

    /** Two slots overlap when they share the day and their time ranges intersect. */
    public boolean overlaps(WeekDay otherDay, LocalTime otherStart, LocalTime otherEnd) {
        return day == otherDay && startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }
}
