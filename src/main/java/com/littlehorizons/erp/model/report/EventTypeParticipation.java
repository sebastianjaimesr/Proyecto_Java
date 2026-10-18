package com.littlehorizons.erp.model.report;

/**
 * Events and participations of one event type in a year.
 */
public record EventTypeParticipation(String eventType, int events, int participations, int distinctStudents) {
}
