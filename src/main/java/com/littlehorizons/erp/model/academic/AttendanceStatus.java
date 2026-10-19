package com.littlehorizons.erp.model.academic;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Estado de asistencia diaria ({@code asistencias.estado_asistencia}).
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static AttendanceStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(AttendanceStatus.class, value);
    }
}
