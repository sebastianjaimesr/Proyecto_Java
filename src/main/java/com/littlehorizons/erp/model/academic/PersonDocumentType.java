package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Document types for adults: guardians and teachers.
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

    /** Value stored in MySQL to enum constant. */
    public static PersonDocumentType fromDbValue(String value) {
        return DbEnum.fromDbValue(PersonDocumentType.class, value);
    }
}
