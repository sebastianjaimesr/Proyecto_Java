package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado de un estudiante ({@code estudiantes.estado}).
 */
public enum StudentStatus implements DbEnum {

    ACTIVE("Activo"),
    GRADUATED("Graduado"),
    WITHDRAWN("Retirado"),
    SUSPENDED("Suspendido");

    private final String dbValue;

    StudentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static StudentStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(StudentStatus.class, value);
    }
}
