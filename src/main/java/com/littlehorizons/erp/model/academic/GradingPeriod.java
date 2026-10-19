package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Período de una nota: I a IV más la nota Final calculada.
 */
public enum GradingPeriod implements DbEnum {

    I("I"),
    II("II"),
    III("III"),
    IV("IV"),
    FINAL("Final");

    private final String dbValue;

    GradingPeriod(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Períodos que un profesor puede digitar; la Final la calcula el sistema. */
    public boolean isEditable() {
        return this != FINAL;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static GradingPeriod fromDbValue(String value) {
        return DbEnum.fromDbValue(GradingPeriod.class, value);
    }
}
