package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Días de clase de la semana que usa {@code horarios_clase.dia_semana} (cambio autorizado P-1).
 */
public enum WeekDay implements DbEnum {

    MONDAY("Lunes"),
    TUESDAY("Martes"),
    WEDNESDAY("Miércoles"),
    THURSDAY("Jueves"),
    FRIDAY("Viernes"),
    SATURDAY("Sábado");

    private final String dbValue;

    WeekDay(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static WeekDay fromDbValue(String value) {
        return DbEnum.fromDbValue(WeekDay.class, value);
    }
}
