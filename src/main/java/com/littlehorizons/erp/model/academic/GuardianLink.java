package com.littlehorizons.erp.model.academic;

/**
 * A guardian as related to one student ({@code estudiante_acudiente}).
 */
public record GuardianLink(Guardian guardian, boolean primary, boolean emergencyContact) {
}
