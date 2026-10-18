package com.littlehorizons.erp.model.academic;

/**
 * A student as related to one guardian ({@code estudiante_acudiente}).
 */
public record StudentLink(Student student, boolean primary, boolean emergencyContact) {
}
