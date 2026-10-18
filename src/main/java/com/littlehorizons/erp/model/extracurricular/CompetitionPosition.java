package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Allowed values for {@code participantes_evento.posicion_obtenida}; the column is free text, so the application restricts it to the four values documented in the official script.
 */
public enum CompetitionPosition implements DbEnum {

    FIRST("1er Lugar"),
    SECOND("2do Lugar"),
    THIRD("3er Lugar"),
    PARTICIPANT("Participante");

    private final String dbValue;

    CompetitionPosition(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Podium positions can be awarded only once per event. */
    public boolean isPodium() {
        return this != PARTICIPANT;
    }

    /** Value stored in MySQL to enum constant. */
    public static CompetitionPosition fromDbValue(String value) {
        return DbEnum.fromDbValue(CompetitionPosition.class, value);
    }
}
