package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado de una inscripción a materia ({@code inscripciones_materia.estado}).
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static RegistrationStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(RegistrationStatus.class, value);
    }
}
