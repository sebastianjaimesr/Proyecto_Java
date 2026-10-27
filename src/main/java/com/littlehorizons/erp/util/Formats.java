package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display formats for Colombian users: pesos with thousands separator, dates
 * and masked documents.
 */
public final class Formats {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", COLOMBIA);
    private static final int VISIBLE_DOCUMENT_DIGITS = 4;

    private Formats() {
    }

    /** "$ 1.250.000" (pesos, no decimals when the amount is whole). */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(COLOMBIA));
        return "$ " + format.format(amount);
    }

    /** Grade with one decimal, or "-" when missing. */
    public static String grade(BigDecimal value) {
        return value == null ? "-" : value.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME.format(dateTime);
    }

    /** "27 de octubre de 2026". */
    public static String longDate(LocalDate date) {
        return date == null ? "-" : LONG_DATE.format(date);
    }

    /** Shows only the last digits of a document in lists: "******5678". */
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

    public static String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }
}
