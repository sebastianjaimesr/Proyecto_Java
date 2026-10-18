package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Status of a club membership ({@code membresias_club.estado_membresia}).
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

    /** Value stored in MySQL to enum constant. */
    public static MembershipStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(MembershipStatus.class, value);
    }
}
