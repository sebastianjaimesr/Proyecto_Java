package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Valores de género de {@code estudiantes.genero}.
 */
public enum Gender implements DbEnum {

    MALE("M"),
    FEMALE("F"),
    OTHER("Otro");

    private final String dbValue;

    Gender(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static Gender fromDbValue(String value) {
        return DbEnum.fromDbValue(Gender.class, value);
    }
}
