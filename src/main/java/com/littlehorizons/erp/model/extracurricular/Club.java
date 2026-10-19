package com.littlehorizons.erp.model.extracurricular;

/**
 * Club extracurricular ({@code clubes}).
 *
 * @param id             {@code null} antes de insertar el registro
 * @param supervisorId   profesor a cargo; puede ser {@code null}
 * @param supervisorName obtenido con join, para mostrar
 * @param capacity       número máximo de miembros activos
 * @param activeMembers  conteo calculado de membresías activas, para mostrar
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

    /** Valor por defecto de {@code clubes.capacidad_maxima} en el esquema oficial. */
    public static final int DEFAULT_CAPACITY = 30;

    /** Cupos libres del club (nunca negativo). */
    public int availableSeats() {
        return Math.max(0, capacity - activeMembers);
    }
}
