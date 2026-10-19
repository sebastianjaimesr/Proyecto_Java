package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Tipos de documento permitidos para estudiantes ({@code estudiantes.tipo_documento}).
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static StudentDocumentType fromDbValue(String value) {
        return DbEnum.fromDbValue(StudentDocumentType.class, value);
    }
}
