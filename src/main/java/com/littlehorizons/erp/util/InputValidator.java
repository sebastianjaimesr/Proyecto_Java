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
 * Reusable validation and parsing of raw text typed by the user.
 *
 * <p>Every method either returns the normalized value or throws a
 * {@link ValidationException} whose message (in Spanish) tells the user what to
 * fix. The class has no console access, so it is used both by the views and by
 * the services, and it is easy to unit test.</p>
 */
public final class InputValidator {

    /** Dates are typed as ISO-8601 to avoid day/month ambiguity. */
    public static final String DATE_PATTERN_LABEL = "AAAA-MM-DD";

    public static final BigDecimal MIN_GRADE = new BigDecimal("0.0");
    public static final BigDecimal MAX_GRADE = new BigDecimal("5.0");
    /** Upper bound of a DECIMAL(10,2) column. */
    public static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_ALLOWED_CHARS = Pattern.compile("^\\+?[0-9 ()-]+$");
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("^[A-Z0-9]{5,20}$");
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]*$");
    /** Plain decimal notation only: no exponent, no thousands separators. */
    private static final Pattern DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MIN_PHONE_DIGITS = 7;
    private static final int MAX_PHONE_DIGITS = 15;
    private static final int MONEY_SCALE = 2;
    private static final int GRADE_SCALE = 1;

    private InputValidator() {
    }

    /** Menu option between {@code min} and {@code max}, both inclusive. */
    public static int parseMenuOption(String raw, int min, int max) {
        String value = trimToEmpty(raw);
        try {
            int option = Integer.parseInt(value);
            if (option >= min && option <= max) {
                return option;
            }
        } catch (NumberFormatException e) {
            // Handled below with the same message as an out of range number.
        }
        throw new ValidationException("Opción inválida. Ingrese un número entre "
                + min + " y " + max + ".");
    }

    /** Mandatory text, trimmed, with a maximum length (usually the column size). */
    public static String requireText(String raw, String fieldName, int maxLength) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            throw new ValidationException("El campo " + fieldName + " es obligatorio.");
        }
        return checkLength(value, fieldName, maxLength);
    }

    /** Optional text: returns {@code null} when it is blank. */
    public static String optionalText(String raw, String fieldName, int maxLength) {
        String value = trimToEmpty(raw);
        return value.isEmpty() ? null : checkLength(value, fieldName, maxLength);
    }

    /** Integer between {@code min} and {@code max}, both inclusive. */
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

    /** Database identifier (positive integer). */
    public static int parseId(String raw, String fieldName) {
        return parseInt(raw, fieldName, 1, Integer.MAX_VALUE);
    }

    /**
     * Decimal number with at most {@code maxScale} decimals. Both "4.5" and
     * "4,5" are accepted; thousands separators are not.
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

    /** Grade between 0.0 and 5.0 with one decimal, as stored in DECIMAL(2,1). */
    public static BigDecimal parseGrade(String raw) {
        BigDecimal grade = parseDecimal(raw, "nota", GRADE_SCALE);
        if (grade.compareTo(MIN_GRADE) < 0 || grade.compareTo(MAX_GRADE) > 0) {
            throw new ValidationException("La nota debe estar entre 0.0 y 5.0.");
        }
        return grade.setScale(GRADE_SCALE);
    }

    /** Non-negative amount in pesos with up to two decimals (DECIMAL(10,2)). */
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

    /** Calendar date in {@value #DATE_PATTERN_LABEL} format; invalid days are rejected. */
    public static LocalDate parseDate(String raw, String fieldName) {
        String value = trimToEmpty(raw);
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException("El campo " + fieldName + " debe ser una fecha válida con formato "
                    + DATE_PATTERN_LABEL + " (ejemplo: 2026-03-15).");
        }
    }

    /** E-mail address, stored in lower case. */
    public static String validateEmail(String raw, String fieldName) {
        String value = trimToEmpty(raw).toLowerCase(Locale.ROOT);
        if (value.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(value).matches()) {
            throw new ValidationException("El campo " + fieldName
                    + " debe ser un correo electrónico válido (ejemplo: nombre@dominio.com).");
        }
        return value;
    }

    /**
     * Phone number: digits with optional leading '+', spaces, dashes or
     * parentheses, and between 7 and 15 digits in total.
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

    /** Identity document number: 5 to 20 letters or digits, stored in upper case. */
    public static String validateDocumentNumber(String raw) {
        String value = trimToEmpty(raw).toUpperCase(Locale.ROOT);
        if (!DOCUMENT_NUMBER.matcher(value).matches()) {
            throw new ValidationException(
                    "El número de documento debe tener entre 5 y 20 letras o dígitos, sin espacios ni puntos.");
        }
        return value;
    }

    /**
     * Catalogue code such as "MAT-5" or "LIB-0001": upper case letters, digits,
     * '-' or '_'. Uniqueness is checked against the database by the services.
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
     * Picks one of the allowed options, either by its 1-based position in the
     * list or by its label (case-insensitive). Used for payment methods,
     * statuses and other closed lists.
     */
    public static <T> T parseChoice(String raw, List<T> options, Function<T, String> labelOf, String fieldName) {
        String value = trimToEmpty(raw);
        try {
            int position = Integer.parseInt(value);
            if (position >= 1 && position <= options.size()) {
                return options.get(position - 1);
            }
        } catch (NumberFormatException e) {
            // Not a position: try to match the label below.
        }
        return options.stream()
                .filter(option -> labelOf.apply(option).equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Valor inválido para " + fieldName
                        + ". Elija un número entre 1 y " + options.size() + "."));
    }

    /** Yes/no answer: accepts s, si, sí, n, no (case-insensitive). */
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
