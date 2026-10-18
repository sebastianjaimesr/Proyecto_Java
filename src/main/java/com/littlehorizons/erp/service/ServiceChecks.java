package com.littlehorizons.erp.service;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Validation helpers shared by the services. Views already validate the
 * format of each field; services check again because they are the last layer
 * before the database and can be called from tests or other views.
 */
final class ServiceChecks {

    static final int MIN_ACADEMIC_YEAR = 2000;
    static final int MAX_YEARS_AHEAD = 1;
    static final LocalTime SCHOOL_DAY_START = LocalTime.of(6, 0);
    static final LocalTime SCHOOL_DAY_END = LocalTime.of(20, 0);

    private ServiceChecks() {
    }

    static <T> T require(Optional<T> value, String notFoundMessage) {
        return value.orElseThrow(() -> new BusinessRuleException(notFoundMessage));
    }

    static void requireText(String value, String fieldName, int maxLength) {
        InputValidator.requireText(value, fieldName, maxLength);
    }

    static void optionalText(String value, String fieldName, int maxLength) {
        InputValidator.optionalText(value, fieldName, maxLength);
    }

    static void optionalEmail(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            InputValidator.validateEmail(value, fieldName);
        }
    }

    static void optionalPhone(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            InputValidator.validatePhone(value, fieldName);
        }
    }

    static void notInFuture(LocalDate date, LocalDate today, String fieldName) {
        if (date == null) {
            throw new ValidationException("El campo " + fieldName + " es obligatorio.");
        }
        if (date.isAfter(today)) {
            throw new ValidationException("El campo " + fieldName + " no puede ser una fecha futura.");
        }
    }

    static void academicYear(int year, LocalDate today) {
        int max = today.getYear() + MAX_YEARS_AHEAD;
        if (year < MIN_ACADEMIC_YEAR || year > max) {
            throw new ValidationException("El año académico debe estar entre " + MIN_ACADEMIC_YEAR + " y " + max + ".");
        }
    }

    /** Sections are stored in upper case so that "a" and "A" are the same group. */
    static String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return null;
        }
        String value = section.strip().toUpperCase(java.util.Locale.ROOT);
        if (value.length() > 10) {
            throw new ValidationException("La sección admite como máximo 10 caracteres.");
        }
        return value;
    }
}
