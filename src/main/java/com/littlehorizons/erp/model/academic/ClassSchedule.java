package com.littlehorizons.erp.model.academic;

import java.time.LocalTime;

/**
 * Una franja semanal de clase de una asignación ({@code horarios_clase}, cambio autorizado P-1).
 *
 * @param id          {@code null} antes de insertar el registro
 * @param description texto obtenido con join "materia - sección - profesor", para mostrar
 */
public record ClassSchedule(
        Integer id,
        int assignmentId,
        WeekDay day,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        String description) {

    /** Dos franjas se cruzan cuando comparten el día y sus rangos de horas se intersectan. */
    public boolean overlaps(WeekDay otherDay, LocalTime otherStart, LocalTime otherEnd) {
        return day == otherDay && startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }
}
