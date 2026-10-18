package com.littlehorizons.erp.model.common;

import java.util.Arrays;

/**
 * Enum whose constants map to the exact text stored in a MySQL ENUM or
 * VARCHAR column. Java constant names are in English; the stored values keep
 * the Spanish labels of the official schema (some contain spaces or accents).
 */
public interface DbEnum {

    /** Exact value stored in the database, also used as the label on screen. */
    String dbValue();

    /**
     * Finds the constant for a stored value.
     *
     * @return the constant, or {@code null} when {@code value} is {@code null}
     * @throws IllegalArgumentException if the value is unknown
     */
    static <E extends Enum<E> & DbEnum> E fromDbValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.dbValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown value '" + value + "' for " + type.getSimpleName()));
    }
}
