package com.littlehorizons.erp.model.finance;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Tipo de ajuste de precio por estudiante ({@code valores_estudiante_concepto.tipo_ajuste}, cambio autorizado P-2).
 */
public enum AdjustmentType implements DbEnum {

    CUSTOM_VALUE("Valor Personalizado"),
    SCHOLARSHIP("Beca"),
    SIBLING_DISCOUNT("Descuento Hermanos");

    private final String dbValue;

    AdjustmentType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static AdjustmentType fromDbValue(String value) {
        return DbEnum.fromDbValue(AdjustmentType.class, value);
    }
}
