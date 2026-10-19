package com.littlehorizons.erp.model.common;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nombre completo colombiano: uno o dos nombres y uno o dos apellidos, tal como
 * se guardan en las columnas {@code primer_nombre}, {@code segundo_nombre},
 * {@code primer_apellido} y {@code segundo_apellido}.
 */
public record PersonName(String firstName, String middleName, String lastName, String secondLastName) {

    public PersonName {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
    }

    /** "Primer Segundo Apellido1 Apellido2", omitiendo las partes que falten. */
    public String fullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    /** "Apellido1 Apellido2, Primer Segundo" para listas ordenadas. */
    public String sortableName() {
        String surnames = Stream.of(lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        String names = Stream.of(firstName, middleName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return surnames + ", " + names;
    }
}
