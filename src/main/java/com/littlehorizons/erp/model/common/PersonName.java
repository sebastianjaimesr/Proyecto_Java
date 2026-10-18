package com.littlehorizons.erp.model.common;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Colombian full name: one or two first names and one or two last names, as
 * stored in the {@code primer_nombre}, {@code segundo_nombre},
 * {@code primer_apellido} and {@code segundo_apellido} columns.
 */
public record PersonName(String firstName, String middleName, String lastName, String secondLastName) {

    public PersonName {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
    }

    /** "Primer Segundo Apellido1 Apellido2", skipping missing parts. */
    public String fullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    /** "Apellido1 Apellido2, Primer Segundo" for sorted lists. */
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
