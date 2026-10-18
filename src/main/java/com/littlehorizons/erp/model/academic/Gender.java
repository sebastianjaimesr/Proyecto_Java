package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Gender values of {@code estudiantes.genero}.
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

    /** Value stored in MySQL to enum constant. */
    public static Gender fromDbValue(String value) {
        return DbEnum.fromDbValue(Gender.class, value);
    }
}
