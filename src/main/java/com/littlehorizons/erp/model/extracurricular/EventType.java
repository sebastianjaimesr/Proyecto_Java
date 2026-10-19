package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Categorías de evento ({@code eventos.tipo_evento}).
 */
public enum EventType implements DbEnum {

    ACADEMIC("Académico"),
    CULTURAL("Cultural"),
    SPORTS("Deportivo"),
    SOCIAL("Social"),
    SCIENTIFIC("Científico");

    private final String dbValue;

    EventType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static EventType fromDbValue(String value) {
        return DbEnum.fromDbValue(EventType.class, value);
    }
}
