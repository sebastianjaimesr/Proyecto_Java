package com.littlehorizons.erp.model.report;

/**
 * Eventos y participaciones de un tipo de evento en un año.
 */
public record EventTypeParticipation(String eventType, int events, int participations, int distinctStudents) {
}
