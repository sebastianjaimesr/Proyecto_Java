package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado de un profesor ({@code profesores.estado}).
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static TeacherStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(TeacherStatus.class, value);
    }
}
