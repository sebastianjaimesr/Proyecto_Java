package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Validación y conversión reutilizables del texto que digita el usuario.
 *
 * <p>Cada método devuelve el valor normalizado o lanza una
 * {@link ValidationException} cuyo mensaje (en español) le dice al usuario qué
 * corregir. La clase no accede a la consola, así que la usan tanto las vistas como
 * los servicios, y es fácil de probar con pruebas unitarias.</p>
 */
public final class InputValidator {

    /** Las fechas se digitan en formato ISO-8601 para evitar la confusión entre día y mes. */
    public static final String DATE_PATTERN_LABEL = "AAAA-MM-DD";

    public static final BigDecimal MIN_GRADE = new BigDecimal("0.0");
    public static final BigDecimal MAX_GRADE = new BigDecimal("5.0");
    /** Límite superior de una columna DECIMAL(10,2). */
    public static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_ALLOWED_CHARS = Pattern.compile("^\\+?[0-9 ()-]+$");
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("^[A-Z0-9]{5,20}$");
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]*$");
    /** Solo notación decimal simple: sin exponente ni separadores de miles. */
    private static final Pattern DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MIN_PHONE_DIGITS = 7;
    private static final int MAX_PHONE_DIGITS = 15;
    private static final int MONEY_SCALE = 2;
    private static final int GRADE_SCALE = 1;

    private InputValidator() {
    }

    /** Opción de menú entre {@code min} y {@code max}, ambos incluidos. */
    public static int parseMenuOption(String raw, int min, int max) {
        String value = trimToEmpty(raw);
        try {
            int option = Integer.parseInt(value);
            if (option >= min && option <= max) {
                return option;
            }
        } catch (NumberFormatException e) {
            // Se maneja más abajo con el mismo mensaje de un número fuera de rango.
        }
        throw new ValidationException("Opción inválida. Ingrese un número entre "
                + min + " y " + max + ".");
    }

    /** Texto obligatorio, sin espacios a los lados, con longitud máxima (normalmente el tamaño de la columna). */
    public static String requireText(String raw, String fieldName, int maxLength) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            throw new ValidationException("El campo " + fieldName + " es obligatorio.");
        }
        return checkLength(value, fieldName, maxLength);
    }

    /** Texto opcional: devuelve {@code null} si está vacío. */
    public static String optionalText(String raw, String fieldName, int maxLength) {
        String value = trimToEmpty(raw);
        return value.isEmpty() ? null : checkLength(value, fieldName, maxLength);
    }

    /** Entero entre {@code min} y {@code max}, ambos incluidos. */
    public static int parseInt(String raw, String fieldName, int min, int max) {
        String value = trimToEmpty(raw);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("El campo " + fieldName + " debe ser un número entero.");
        }
        if (number < min || number > max) {
            throw new ValidationException("El campo " + fieldName + " debe estar entre "
                    + min + " y " + max + ".");
        }
        return number;
    }

    /** Identificador de la base de datos (entero positivo). */
    public static int parseId(String raw, String fieldName) {
        return parseInt(raw, fieldName, 1, Integer.MAX_VALUE);
    }

    /**
     * Número decimal con máximo {@code maxScale} decimales. Se aceptan "4.5" y
     * "4,5"; los separadores de miles no.
     */
    public static BigDecimal parseDecimal(String raw, String fieldName, int maxScale) {
        String value = trimToEmpty(raw);
        if (value.indexOf(',') >= 0 && value.indexOf('.') < 0) {
            value = value.replace(',', '.');
        }
        if (!DECIMAL.matcher(value).matches()) {
            throw new ValidationException("El campo " + fieldName
                    + " debe ser un número válido, sin separadores de miles (ejemplo: 4.5).");
        }
        BigDecimal number = new BigDecimal(value);
        if (number.stripTrailingZeros().scale() > maxScale) {
            throw new ValidationException("El campo " + fieldName + " admite como máximo "
                    + maxScale + (maxScale == 1 ? " decimal." : " decimales."));
        }
        return number;
    }

    /** Nota entre 0.0 y 5.0 con un decimal, como se guarda en DECIMAL(2,1). */
    public static BigDecimal parseGrade(String raw) {
        BigDecimal grade = parseDecimal(raw, "nota", GRADE_SCALE);
        if (grade.compareTo(MIN_GRADE) < 0 || grade.compareTo(MAX_GRADE) > 0) {
            throw new ValidationException("La nota debe estar entre 0.0 y 5.0.");
        }
        return grade.setScale(GRADE_SCALE);
    }

    /** Monto no negativo en pesos con hasta dos decimales (DECIMAL(10,2)). */
    public static BigDecimal parseMoney(String raw, String fieldName) {
        BigDecimal amount = parseDecimal(raw, fieldName, MONEY_SCALE);
        if (amount.signum() < 0) {
            throw new ValidationException("El campo " + fieldName + " no puede ser negativo.");
        }
        if (amount.compareTo(MAX_MONEY) > 0) {
            throw new ValidationException("El campo " + fieldName + " supera el valor máximo permitido ("
                    + MAX_MONEY.toPlainString() + ").");
        }
        return amount.setScale(MONEY_SCALE);
    }

    /** Fecha del calendario en formato {@value #DATE_PATTERN_LABEL}; los días inválidos se rechazan. */
    public static LocalDate parseDate(String raw, String fieldName) {
        String value = trimToEmpty(raw);
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException("El campo " + fieldName + " debe ser una fecha válida con formato "
                    + DATE_PATTERN_LABEL + " (ejemplo: 2026-03-15).");
        }
    }

    /** Correo electrónico, guardado en minúsculas. */
    public static String validateEmail(String raw, String fieldName) {
        String value = trimToEmpty(raw).toLowerCase(Locale.ROOT);
        if (value.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(value).matches()) {
            throw new ValidationException("El campo " + fieldName
                    + " debe ser un correo electrónico válido (ejemplo: nombre@dominio.com).");
        }
        return value;
    }

    /**
     * Número de teléfono: dígitos con '+' inicial opcional, espacios, guiones o
     * paréntesis, y entre 7 y 15 dígitos en total.
     */
    public static String validatePhone(String raw, String fieldName) {
        String value = trimToEmpty(raw);
        long digits = value.chars().filter(Character::isDigit).count();
        if (value.length() > MAX_PHONE_LENGTH
                || !PHONE_ALLOWED_CHARS.matcher(value).matches()
                || digits < MIN_PHONE_DIGITS || digits > MAX_PHONE_DIGITS) {
            throw new ValidationException("El campo " + fieldName
                    + " debe ser un teléfono válido de 7 a 15 dígitos (ejemplo: 3001234567).");
        }
        return value;
    }

    /** Número de documento de identidad: de 5 a 20 letras o dígitos, guardado en mayúsculas. */
    public static String validateDocumentNumber(String raw) {
        String value = trimToEmpty(raw).toUpperCase(Locale.ROOT);
        if (!DOCUMENT_NUMBER.matcher(value).matches()) {
            throw new ValidationException(
                    "El número de documento debe tener entre 5 y 20 letras o dígitos, sin espacios ni puntos.");
        }
        return value;
    }

    /**
     * Código de catálogo como "MAT-5" o "LIB-0001": letras mayúsculas, dígitos,
     * '-' o '_'. Los servicios verifican contra la base de datos que no esté repetido.
     */
    public static String validateCode(String raw, String fieldName, int maxLength) {
        String value = trimToEmpty(raw).toUpperCase(Locale.ROOT);
        if (value.isEmpty() || !CODE.matcher(value).matches()) {
            throw new ValidationException("El campo " + fieldName
                    + " solo admite letras, números, '-' y '_' (ejemplo: MAT-5).");
        }
        return checkLength(value, fieldName, maxLength);
    }

    /**
     * Elige una de las opciones permitidas, por su posición en la lista (empezando en 1)
     * o por su etiqueta (sin distinguir mayúsculas). Se usa para métodos de pago,
     * estados y otras listas cerradas.
     */
    public static <T> T parseChoice(String raw, List<T> options, Function<T, String> labelOf, String fieldName) {
        String value = trimToEmpty(raw);
        try {
            int position = Integer.parseInt(value);
            if (position >= 1 && position <= options.size()) {
                return options.get(position - 1);
            }
        } catch (NumberFormatException e) {
            // No es una posición: se intenta buscar por etiqueta más abajo.
        }
        return options.stream()
                .filter(option -> labelOf.apply(option).equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Valor inválido para " + fieldName
                        + ". Elija un número entre 1 y " + options.size() + "."));
    }

    /** Respuesta sí/no: acepta s, si, sí, n, no (sin distinguir mayúsculas). */
    public static boolean parseYesNo(String raw) {
        String value = trimToEmpty(raw).toLowerCase(Locale.ROOT);
        switch (value) {
            case "s":
            case "si":
            case "sí":
                return true;
            case "n":
            case "no":
                return false;
            default:
                throw new ValidationException("Responda S (sí) o N (no).");
        }
    }

    private static String checkLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new ValidationException("El campo " + fieldName + " admite como máximo "
                    + maxLength + " caracteres.");
        }
        return value;
    }

    private static String trimToEmpty(String raw) {
        return raw == null ? "" : raw.strip();
    }
}
