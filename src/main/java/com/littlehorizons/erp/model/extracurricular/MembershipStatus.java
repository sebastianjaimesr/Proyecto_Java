package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado de una membresía de club ({@code membresias_club.estado_membresia}).
 */
public enum MembershipStatus implements DbEnum {

    ACTIVE("Activa"),
    WITHDRAWN("Retirada");

    private final String dbValue;

    MembershipStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static MembershipStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(MembershipStatus.class, value);
    }
}
