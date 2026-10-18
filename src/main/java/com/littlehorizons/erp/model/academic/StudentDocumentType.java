package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Document types allowed for students ({@code estudiantes.tipo_documento}).
 */
public enum StudentDocumentType implements DbEnum {

    TI("TI"),
    CC("CC"),
    CE("CE"),
    PASSPORT("Pasaporte");

    private final String dbValue;

    StudentDocumentType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static StudentDocumentType fromDbValue(String value) {
        return DbEnum.fromDbValue(StudentDocumentType.class, value);
    }
}
