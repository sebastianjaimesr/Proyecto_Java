package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.model.common.DbEnum;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.InputValidator;

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
 * Preguntas compartidas por las vistas de los módulos: mostrar resultados, elegir registros de
 * una búsqueda, leer nombres, enums, años y valores opcionales.
 */
public class ViewSupport {

    private static final int MAX_NAME_LENGTH = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final ConsoleUtils console;

    public ViewSupport(ConsoleUtils console) {
        this.console = Objects.requireNonNull(console, "console");
    }

    /** Consola compartida por las vistas. */
    public ConsoleUtils console() {
        return console;
    }

    /** Imprime [OK] o [ERROR] con el mensaje y devuelve los datos si tuvo éxito. */
    public <T> Optional<T> show(OperationResult<T> result) {
        if (result.success()) {
            console.printSuccess(result.message());
            return result.dataOptional();
        }
        console.printError(result.message());
        return Optional.empty();
    }

    /** Como {@link #show}, pero solo imprime los errores; se usa en las búsquedas. */
    public <T> Optional<T> quiet(OperationResult<T> result) {
        if (!result.success()) {
            console.printError(result.message());
            return Optional.empty();
        }
        return result.dataOptional();
    }

    /**
     * Pide un texto de búsqueda, muestra las coincidencias y devuelve la elegida:
     * directamente si hay una sola coincidencia; si no, por su id.
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

    /** Lee primer nombre, segundo nombre, primer apellido y segundo apellido; las partes opcionales pueden quedar vacías. */
    public PersonName readName(PersonName current) {
        String first = readTextOrKeep("Primer nombre", current == null ? null : current.firstName(), true);
        String middle = readTextOrKeep("Segundo nombre (opcional)", current == null ? null : current.middleName(), false);
        String last = readTextOrKeep("Primer apellido", current == null ? null : current.lastName(), true);
        String secondLast = readTextOrKeep("Segundo apellido (opcional)",
                current == null ? null : current.secondLastName(), false);
        return new PersonName(first, middle, last, secondLast);
    }

    /**
     * Lee un texto. Si {@code current} existe, Enter lo conserva; en los campos
     * opcionales "-" lo borra.
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

    /** Texto opcional validado con {@code parser}; Enter conserva {@code current} (o lo deja vacío). */
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

    /** Elige una constante de un {@link DbEnum}, mostrando sus etiquetas en español. */
    public <E extends Enum<E> & DbEnum> E readEnum(String label, Class<E> type) {
        return readEnum(label, Arrays.asList(type.getEnumConstants()));
    }

    public <E extends Enum<E> & DbEnum> E readEnum(String label, List<E> options) {
        console.println(label + ":");
        return console.readChoice("Opción: ", options, DbEnum::dbValue, label.toLowerCase(java.util.Locale.ROOT));
    }

    /** Lee un año; Enter usa el año actual. */
    public int readYear(String label) {
        int current = LocalDate.now().getYear();
        return console.readValid(label + " [" + current + "]: ", raw -> raw.isBlank()
                ? current : InputValidator.parseInt(raw, "año", 2000, current + 1));
    }

    /** Fecha con hoy como valor por defecto. */
    public LocalDate readDateOrToday(String label) {
        LocalDate today = LocalDate.now();
        return console.readValid(label + " (" + InputValidator.DATE_PATTERN_LABEL + ") [" + today + "]: ",
                raw -> raw.isBlank() ? today : InputValidator.parseDate(raw, label.toLowerCase(java.util.Locale.ROOT)));
    }

    /** Lee una fecha en formato AAAA-MM-DD. */
    public LocalDate readDate(String label) {
        return console.readDate(label + " (" + InputValidator.DATE_PATTERN_LABEL + "): ",
                label.toLowerCase(java.util.Locale.ROOT));
    }

    /** Hora en formato de 24 horas, por ejemplo 7:00 o 14:30. */
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

    /** Lee una sección opcional (por ejemplo, A); vacío significa sin sección. */
    public String readSection(String label) {
        String raw = console.readLine(label + " (opcional, ej: A): ").strip();
        return raw.isEmpty() ? null : raw;
    }

    /** Avisa que la operación se canceló (por ejemplo, cuando no se eligió ningún registro). */
    public void notFound() {
        console.printWarning("Operación cancelada.");
    }
}
