package com.littlehorizons.erp.model.finance;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Period a payment applies to ({@code pagos.periodo_academico}).
 */
public enum PaymentPeriod implements DbEnum {

    I("I"),
    II("II"),
    III("III"),
    IV("IV"),
    ANNUAL("Anual");

    private final String dbValue;

    PaymentPeriod(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static PaymentPeriod fromDbValue(String value) {
        return DbEnum.fromDbValue(PaymentPeriod.class, value);
    }
}
