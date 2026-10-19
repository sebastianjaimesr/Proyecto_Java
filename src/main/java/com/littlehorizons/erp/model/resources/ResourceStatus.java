package com.littlehorizons.erp.model.resources;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado del inventario ({@code recursos.estado_recurso}).
 */
public enum ResourceStatus implements DbEnum {

    AVAILABLE("Disponible"),
    LOANED("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    RETIRED("Dado de Baja");

    private final String dbValue;

    ResourceStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static ResourceStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(ResourceStatus.class, value);
    }
}
