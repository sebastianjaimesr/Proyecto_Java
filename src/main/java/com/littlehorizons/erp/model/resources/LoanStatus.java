package com.littlehorizons.erp.model.resources;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Loan status in both loan tables.
 */
public enum LoanStatus implements DbEnum {

    ACTIVE("Activo"),
    RETURNED("Devuelto"),
    OVERDUE("Vencido");

    private final String dbValue;

    LoanStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static LoanStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(LoanStatus.class, value);
    }
}
