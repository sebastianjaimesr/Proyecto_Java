package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Period of a grade record: I to IV plus the computed Final grade.
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

    /** Periods a teacher can type; Final is computed by the system. */
    public boolean isEditable() {
        return this != FINAL;
    }

    /** Value stored in MySQL to enum constant. */
    public static GradingPeriod fromDbValue(String value) {
        return DbEnum.fromDbValue(GradingPeriod.class, value);
    }
}
