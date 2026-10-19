package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado de una matrícula anual ({@code matriculas.estado_matricula}).
 */
public enum EnrollmentStatus implements DbEnum {

    ACTIVE("Activa"),
    COMPLETED("Completada"),
    WITHDRAWN("Retirada");

    private final String dbValue;

    EnrollmentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static EnrollmentStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(EnrollmentStatus.class, value);
    }
}
