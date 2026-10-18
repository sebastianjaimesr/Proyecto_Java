package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Daily attendance status ({@code asistencias.estado_asistencia}).
 */
public enum AttendanceStatus implements DbEnum {

    PRESENT("Presente"),
    ABSENT("Ausente"),
    LATE("Tardanza"),
    MEDICAL_EXCUSE("Excusa Médica");

    private final String dbValue;

    AttendanceStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static AttendanceStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(AttendanceStatus.class, value);
    }
}
