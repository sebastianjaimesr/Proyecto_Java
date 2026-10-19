package com.littlehorizons.erp.model.academic;

/**
 * Un estudiante visto desde su relación con un acudiente ({@code estudiante_acudiente}).
 */
public record StudentLink(Student student, boolean primary, boolean emergencyContact) {
}
