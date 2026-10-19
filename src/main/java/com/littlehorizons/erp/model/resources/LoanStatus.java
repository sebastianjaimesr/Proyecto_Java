package com.littlehorizons.erp.model.resources;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado del préstamo en ambas tablas de préstamos.
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static LoanStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(LoanStatus.class, value);
    }
}
