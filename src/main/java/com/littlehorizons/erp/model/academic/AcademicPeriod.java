package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Período académico de una inscripción a materia (I a IV).
 */
public enum AcademicPeriod implements DbEnum {

    I("I"),
    II("II"),
    III("III"),
    IV("IV");

    private final String dbValue;

    AcademicPeriod(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static AcademicPeriod fromDbValue(String value) {
        return DbEnum.fromDbValue(AcademicPeriod.class, value);
    }
}
