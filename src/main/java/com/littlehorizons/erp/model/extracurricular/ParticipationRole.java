package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Rol de un estudiante en un evento ({@code participantes_evento.rol_participacion}).
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static ParticipationRole fromDbValue(String value) {
        return DbEnum.fromDbValue(ParticipationRole.class, value);
    }
}
