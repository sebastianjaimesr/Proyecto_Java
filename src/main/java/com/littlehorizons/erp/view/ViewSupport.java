package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.model.common.DbEnum;
import com.littlehorizons.erp.model.common.ImportResult;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.InputValidator;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prompts shared by the module views: showing results, choosing records from
 * a search, reading names, enums, years and optional values.
 */
public class ViewSupport {

    private static final int MAX_NAME_LENGTH = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final ConsoleUtils console;

    public ViewSupport(ConsoleUtils console) {
        this.console = Objects.requireNonNull(console, "console");
    }

    public ConsoleUtils console() {
        return console;
    }

    /** Prints [OK] or [ERROR] with the message and returns the data when it succeeded. */
    public <T> Optional<T> show(OperationResult<T> result) {
        if (result.success()) {
            console.printSuccess(result.message());
            return result.dataOptional();
        }
        console.printError(result.message());
        return Optional.empty();
    }

    /** Like {@link #show} but prints only errors; used for lookups. */
    public <T> Optional<T> quiet(OperationResult<T> result) {
        if (!result.success()) {
            console.printError(result.message());
            return Optional.empty();
        }
        return result.dataOptional();
    }

    /**
     * Asks for a search text, shows the matches and returns the chosen one:
     * directly when there is a single match, otherwise by its id.
     */
    public <T> Optional<T> pick(String entityName, Function<String, OperationResult<List<T>>> search,
                                Function<T, Integer> idOf, List<String> headers,
                                Function<T, List<String>> rowOf) {
        String text = console.readLine("Buscar " + entityName + " por nombre o documento (Enter para listar): ");
        Optional<List<T>> found = quiet(search.apply(text));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        List<T> matches = found.get();
        if (matches.isEmpty()) {
            console.printWarning("No se encontró ningún registro de " + entityName + ".");
            return Optional.empty();
        }
        console.printTable(headers, matches.stream().map(rowOf).collect(Collectors.toList()));
        if (matches.size() == 1) {
            T only = matches.get(0);
            return console.confirm("¿Usar el registro con id " + idOf.apply(only) + "?")
                    ? Optional.of(only) : Optional.empty();
        }
        int id = console.readId("Id de " + entityName + " (0 para cancelar): ", "id");
        return matches.stream().filter(item -> idOf.apply(item) == id).findFirst()
                .or(() -> {
                    console.printWarning("El id " + id + " no está en la lista.");
                    return Optional.empty();
                });
    }

    /** Reads first, middle, last and second last name; optional parts may be left blank. */
    public PersonName readName(PersonName current) {
        String first = readTextOrKeep("Primer nombre", current == null ? null : current.firstName(), true);
        String middle = readTextOrKeep("Segundo nombre (opcional)", current == null ? null : current.middleName(), false);
        String last = readTextOrKeep("Primer apellido", current == null ? null : current.lastName(), true);
        String secondLast = readTextOrKeep("Segundo apellido (opcional)",
                current == null ? null : current.secondLastName(), false);
        return new PersonName(first, middle, last, secondLast);
    }

    /**
     * Reads a text. When {@code current} exists, Enter keeps it; for optional
     * fields "-" clears it.
     */
    public String readTextOrKeep(String label, String current, boolean required) {
        String prompt = current == null ? label + ": " : label + " [" + current + "]: ";
        while (true) {
            String line = console.readLine(prompt).strip();
            if (line.isEmpty() && current != null) {
                return current;
            }
            if (!required && line.equals("-")) {
                return null;
            }
            if (line.isEmpty() && !required) {
                return null;
            }
            try {
                return InputValidator.requireText(line, label.toLowerCase(java.util.Locale.ROOT), MAX_NAME_LENGTH * 10);
            } catch (com.littlehorizons.erp.exception.ValidationException e) {
                console.printError(e.getMessage());
            }
        }
    }

    /** Optional text validated with {@code parser}; Enter keeps {@code current} (or leaves it empty). */
    public String readOptional(String label, String current, Function<String, String> parser) {
        String prompt = current == null ? label + " (opcional): " : label + " [" + current + "] ('-' para borrar): ";
        return console.readValid(prompt, raw -> {
            String value = raw.strip();
            if (value.isEmpty()) {
                return current;
            }
            if (value.equals("-")) {
                return null;
            }
            return parser.apply(value);
        });
    }

    /** Chooses one constant of a {@link DbEnum}, showing its Spanish labels. */
    public <E extends Enum<E> & DbEnum> E readEnum(String label, Class<E> type) {
        return readEnum(label, Arrays.asList(type.getEnumConstants()));
    }

    public <E extends Enum<E> & DbEnum> E readEnum(String label, List<E> options) {
        console.println(label + ":");
        return console.readChoice("Opción: ", options, DbEnum::dbValue, label.toLowerCase(java.util.Locale.ROOT));
    }

    public int readYear(String label) {
        int current = LocalDate.now().getYear();
        return console.readValid(label + " [" + current + "]: ", raw -> raw.isBlank()
                ? current : InputValidator.parseInt(raw, "año", 2000, current + 1));
    }

    /** Date with today as the default value. */
    public LocalDate readDateOrToday(String label) {
        LocalDate today = LocalDate.now();
        return console.readValid(label + " (" + InputValidator.DATE_PATTERN_LABEL + ") [" + today + "]: ",
                raw -> raw.isBlank() ? today : InputValidator.parseDate(raw, label.toLowerCase(java.util.Locale.ROOT)));
    }

    public LocalDate readDate(String label) {
        return console.readDate(label + " (" + InputValidator.DATE_PATTERN_LABEL + "): ",
                label.toLowerCase(java.util.Locale.ROOT));
    }

    /** Time in 24-hour format, for example 7:00 or 14:30. */
    public LocalTime readTime(String label) {
        return console.readValid(label + " (HH:MM, 24 horas): ", raw -> {
            try {
                return LocalTime.parse(raw.strip(), TIME_FORMAT);
            } catch (DateTimeParseException e) {
                throw new com.littlehorizons.erp.exception.ValidationException(
                        "La hora debe tener el formato HH:MM, por ejemplo 7:00 o 14:30.");
            }
        });
    }

    public String readSection(String label) {
        String raw = console.readLine(label + " (opcional, ej: A): ").strip();
        return raw.isEmpty() ? null : raw;
    }

    /** Path of a file to read, relative to the project folder or absolute. */
    public Path readPath(String label) {
        String raw = console.readValid(label + " (ruta del archivo): ",
                text -> InputValidator.requireText(text.strip().replaceAll("^\"|\"$", ""), "ruta", 500));
        return Path.of(raw);
    }

    /** Shows the import summary and every rejected line. */
    public void showImport(OperationResult<ImportResult> result) {
        show(result).ifPresent(importResult -> importResult.errors().forEach(console::printWarning));
    }

    public void notFound() {
        console.printWarning("Operación cancelada.");
    }
}
