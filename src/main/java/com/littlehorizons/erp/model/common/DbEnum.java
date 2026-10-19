package com.littlehorizons.erp.model.common;

import java.util.Arrays;

/**
 * Enum cuyas constantes corresponden al texto exacto guardado en una columna ENUM o
 * VARCHAR de MySQL. Los nombres de las constantes en Java están en inglés; los valores guardados
 * conservan las etiquetas en español del esquema oficial (algunas tienen espacios o tildes).
 */
public interface DbEnum {

    /** Valor exacto guardado en la base de datos; también se usa como etiqueta en pantalla. */
    String dbValue();

    /**
     * Busca la constante que corresponde a un valor guardado.
     *
     * @return la constante, o {@code null} cuando {@code value} es {@code null}
     * @throws IllegalArgumentException si el valor es desconocido
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
