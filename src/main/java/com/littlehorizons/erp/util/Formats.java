package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos de presentación para usuarios colombianos: pesos con separador de miles, fechas
 * y documentos enmascarados.
 */
public final class Formats {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int VISIBLE_DOCUMENT_DIGITS = 4;

    private Formats() {
    }

    /** "$ 1.250.000" (pesos, sin decimales cuando el monto es entero). */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(COLOMBIA));
        return "$ " + format.format(amount);
    }

    /** Nota con un decimal, o "-" si no existe. */
    public static String grade(BigDecimal value) {
        return value == null ? "-" : value.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Fecha en formato dd/MM/yyyy, o "-" si no hay fecha. */
    public static String date(LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    /** Fecha y hora en formato dd/MM/yyyy HH:mm, o "-" si no hay. */
    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME.format(dateTime);
    }

    /** En las listas muestra solo los últimos dígitos de un documento: "******5678". */
    public static String maskDocument(String document) {
        if (document == null) {
            return "-";
        }
        if (document.length() <= VISIBLE_DOCUMENT_DIGITS) {
            return document;
        }
        return "*".repeat(document.length() - VISIBLE_DOCUMENT_DIGITS)
                + document.substring(document.length() - VISIBLE_DOCUMENT_DIGITS);
    }

    /** El texto, o "-" si está vacío. */
    public static String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** "Sí" o "No". */
    public static String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }
}
