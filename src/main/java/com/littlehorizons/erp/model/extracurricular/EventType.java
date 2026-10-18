package com.littlehorizons.erp.model.extracurricular;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Event categories ({@code eventos.tipo_evento}).
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

    /** Value stored in MySQL to enum constant. */
    public static EventType fromDbValue(String value) {
        return DbEnum.fromDbValue(EventType.class, value);
    }
}
