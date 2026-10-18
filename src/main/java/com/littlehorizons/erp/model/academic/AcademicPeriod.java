package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Academic period of a subject registration (I to IV).
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

    /** Value stored in MySQL to enum constant. */
    public static AcademicPeriod fromDbValue(String value) {
        return DbEnum.fromDbValue(AcademicPeriod.class, value);
    }
}
