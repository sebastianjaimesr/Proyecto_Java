package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * School days of the week used by {@code horarios_clase.dia_semana} (authorized change P-1).
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

    /** Value stored in MySQL to enum constant. */
    public static WeekDay fromDbValue(String value) {
        return DbEnum.fromDbValue(WeekDay.class, value);
    }
}
