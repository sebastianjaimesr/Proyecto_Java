package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Valores permitidos para {@code participantes_evento.posicion_obtenida}; la columna es texto libre, así que la aplicación la limita a los cuatro valores documentados en el script oficial.
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

    /** Las posiciones de podio solo se pueden otorgar una vez por evento. */
    public boolean isPodium() {
        return this != PARTICIPANT;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static CompetitionPosition fromDbValue(String value) {
        return DbEnum.fromDbValue(CompetitionPosition.class, value);
    }
}
