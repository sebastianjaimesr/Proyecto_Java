package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Tipos de documento para adultos: acudientes y profesores.
 */
public enum PersonDocumentType implements DbEnum {

    CC("CC"),
    CE("CE"),
    PASSPORT("Pasaporte");

    private final String dbValue;

    PersonDocumentType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static PersonDocumentType fromDbValue(String value) {
        return DbEnum.fromDbValue(PersonDocumentType.class, value);
    }
}
