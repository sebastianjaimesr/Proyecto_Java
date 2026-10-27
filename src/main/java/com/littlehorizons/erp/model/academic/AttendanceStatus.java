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

    /** P, A, T or E (any case), as written on attendance sheets; {@code null} for anything else. */
    public static AttendanceStatus fromLetter(String letter) {
        if (letter == null) {
            return null;
        }
        switch (letter.strip().toUpperCase(java.util.Locale.ROOT)) {
            case "P":
                return PRESENT;
            case "A":
                return ABSENT;
            case "T":
                return LATE;
            case "E":
                return MEDICAL_EXCUSE;
            default:
                return null;
        }
    }

    /** Value stored in MySQL to enum constant. */
    public static AttendanceStatus fromDbValue(String value) {
        return DbEnum.fromDbValue(AttendanceStatus.class, value);
    }
}
