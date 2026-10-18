package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Status of a subject registration ({@code inscripciones_materia.estado}).
 */
public enum RegistrationStatus implements DbEnum {

    ACTIVE("Activa"),
    WITHDRAWN("Retirada"),
    COMPLETED("Completada");

    private final String dbValue;

    RegistrationStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static RegistrationStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(RegistrationStatus.class, value);
    }
}
