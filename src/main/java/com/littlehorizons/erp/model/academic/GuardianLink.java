package com.littlehorizons.erp.model.academic;

/**
 * Un acudiente visto desde su relación con un estudiante ({@code estudiante_acudiente}).
 */
public record GuardianLink(Guardian guardian, boolean primary, boolean emergencyContact) {
}
