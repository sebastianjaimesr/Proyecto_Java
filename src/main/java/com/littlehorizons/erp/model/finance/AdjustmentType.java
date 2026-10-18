package com.littlehorizons.erp.model.finance;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Type of per-student price adjustment ({@code valores_estudiante_concepto.tipo_ajuste}, authorized change P-2).
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

    /** Value stored in MySQL to enum constant. */
    public static AdjustmentType fromDbValue(String value) {
        return DbEnum.fromDbValue(AdjustmentType.class, value);
    }
}
