package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Status of a student ({@code estudiantes.estado}).
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

    /** Value stored in MySQL to enum constant. */
    public static StudentStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(StudentStatus.class, value);
    }
}
