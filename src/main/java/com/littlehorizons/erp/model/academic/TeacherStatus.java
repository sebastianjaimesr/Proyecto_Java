package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Status of a teacher ({@code profesores.estado}).
 */
public enum TeacherStatus implements DbEnum {

    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    LEAVE("Licencia");

    private final String dbValue;

    TeacherStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static TeacherStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(TeacherStatus.class, value);
    }
}
