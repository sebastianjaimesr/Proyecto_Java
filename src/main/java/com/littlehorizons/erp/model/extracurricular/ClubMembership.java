package com.littlehorizons.erp.model.extracurricular;

import java.time.LocalDate;

/**
 * Membresía de un estudiante en un club ({@code membresias_club}).
 *
 * @param id          {@code null} antes de insertar el registro
 * @param studentName obtenido con join, para mostrar
 * @param clubName    obtenido con join, para mostrar
 */
public record ClubMembership(
        Integer id,
        int studentId,
        String studentName,
        int clubId,
        String clubName,
        LocalDate joinDate,
        MembershipStatus status,
        LocalDate withdrawalDate,
        String withdrawalReason) {
}
