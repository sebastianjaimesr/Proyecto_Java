package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Role of a student in an event ({@code participantes_evento.rol_participacion}).
 */
public enum ParticipationRole implements DbEnum {

    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String dbValue;

    ParticipationRole(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static ParticipationRole fromDbValue(String value) {
        return DbEnum.fromDbValue(ParticipationRole.class, value);
    }
}
