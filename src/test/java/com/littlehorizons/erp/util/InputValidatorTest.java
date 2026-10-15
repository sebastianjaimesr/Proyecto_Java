package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    @Test
    @DisplayName("Menu option inside the range is accepted")
    void menuOptionInRangeIsAccepted() {
        assertEquals(3, InputValidator.parseMenuOption(" 3 ", 1, 9));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "10", "abc", "2.5"})
    void menuOptionOutOfRangeOrNotNumericIsRejected(String raw) {
        assertThrows(ValidationException.class, () -> InputValidator.parseMenuOption(raw, 1, 9));
    }

    @Test
    void requiredTextIsTrimmedAndBlankIsRejected() {
        assertEquals("Ana", InputValidator.requireText("  Ana  ", "nombre", 100));
        ValidationException error = assertThrows(ValidationException.class,
                () -> InputValidator.requireText("   ", "nombre", 100));
        assertTrue(error.getMessage().contains("obligatorio"));
    }

    @Test
    void textLongerThanColumnIsRejected() {
        assertThrows(ValidationException.class, () -> InputValidator.requireText("abcdef", "código", 5));
    }

    @Test
    void optionalTextReturnsNullWhenBlank() {
        assertNull(InputValidator.optionalText("  ", "segundo nombre", 100));
        assertEquals("María", InputValidator.optionalText("María", "segundo nombre", 100));
    }

    @Test
    void integerRangeIsEnforced() {
        assertEquals(2026, InputValidator.parseInt("2026", "año", 2000, 2100));
        assertThrows(ValidationException.class, () -> InputValidator.parseInt("1999", "año", 2000, 2100));
        assertThrows(ValidationException.class, () -> InputValidator.parseInt("dos mil", "año", 2000, 2100));
    }

    @Test
    void idMustBePositive() {
        assertEquals(15, InputValidator.parseId("15", "id"));
        assertThrows(ValidationException.class, () -> InputValidator.parseId("0", "id"));
        assertThrows(ValidationException.class, () -> InputValidator.parseId("-4", "id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "3.5", "3,5", "4.50", "5", "5.0"})
    void validGradesAreAcceptedWithOneDecimal(String raw) {
        BigDecimal grade = InputValidator.parseGrade(raw);
        assertEquals(1, grade.scale());
        assertTrue(grade.compareTo(BigDecimal.ZERO) >= 0 && grade.compareTo(new BigDecimal("5.0")) <= 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "5.1", "4.55", "10", "abc", "", "1e0", "4.5.1"})
    void invalidGradesAreRejected(String raw) {
        assertThrows(ValidationException.class, () -> InputValidator.parseGrade(raw));
    }

    @Test
    void gradeWithCommaIsNormalized() {
        assertEquals(new BigDecimal("3.5"), InputValidator.parseGrade("3,5"));
    }

    @Test
    void moneyAcceptsTwoDecimalsAndRejectsNegativesAndOverflow() {
        assertEquals(new BigDecimal("350000.00"), InputValidator.parseMoney("350000", "monto"));
        assertEquals(new BigDecimal("0.00"), InputValidator.parseMoney("0", "monto"));
        assertThrows(ValidationException.class, () -> InputValidator.parseMoney("-1", "monto"));
        assertThrows(ValidationException.class, () -> InputValidator.parseMoney("10.123", "monto"));
        assertThrows(ValidationException.class, () -> InputValidator.parseMoney("100000000", "monto"));
        assertThrows(ValidationException.class, () -> InputValidator.parseMoney("350.000,00", "monto"));
    }

    @Test
    void validDateIsParsed() {
        assertEquals(LocalDate.of(2024, 2, 29), InputValidator.parseDate("2024-02-29", "fecha"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-02-29", "2026-13-01", "15/03/2026", "2026-3-5", "", "hoy"})
    void invalidDatesAreRejected(String raw) {
        assertThrows(ValidationException.class, () -> InputValidator.parseDate(raw, "fecha"));
    }

    @Test
    void emailIsValidatedAndLowerCased() {
        assertEquals("ana.perez@colegio.edu.co", InputValidator.validateEmail(" Ana.Perez@Colegio.edu.co ", "correo"));
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail("ana@", "correo"));
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail("ana perez@x.com", "correo"));
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail("ana@dominio", "correo"));
    }

    @Test
    void phoneNumbersAreValidated() {
        assertEquals("3001234567", InputValidator.validatePhone("3001234567", "teléfono"));
        assertEquals("+57 (601) 555-1234", InputValidator.validatePhone("+57 (601) 555-1234", "teléfono"));
        assertThrows(ValidationException.class, () -> InputValidator.validatePhone("12345", "teléfono"));
        assertThrows(ValidationException.class, () -> InputValidator.validatePhone("300-ABC-4567", "teléfono"));
    }

    @Test
    void documentNumberIsUpperCasedAndValidated() {
        assertEquals("AB12345", InputValidator.validateDocumentNumber("ab12345"));
        assertThrows(ValidationException.class, () -> InputValidator.validateDocumentNumber("1.023.456"));
        assertThrows(ValidationException.class, () -> InputValidator.validateDocumentNumber("123"));
    }

    @Test
    void codesAreUpperCasedAndValidated() {
        assertEquals("MAT-5", InputValidator.validateCode("mat-5", "código", 20));
        assertThrows(ValidationException.class, () -> InputValidator.validateCode("MAT 5", "código", 20));
        assertThrows(ValidationException.class, () -> InputValidator.validateCode("-MAT", "código", 20));
        assertThrows(ValidationException.class, () -> InputValidator.validateCode("ABCDEFGHIJK", "código", 10));
    }

    @Test
    void choiceCanBeSelectedByPositionOrLabel() {
        List<String> methods = List.of("Efectivo", "Transferencia", "Tarjeta Débito");
        Function<String, String> label = Function.identity();
        assertEquals("Transferencia", InputValidator.parseChoice("2", methods, label, "método de pago"));
        assertEquals("Tarjeta Débito", InputValidator.parseChoice("tarjeta débito", methods, label, "método de pago"));
        assertThrows(ValidationException.class,
                () -> InputValidator.parseChoice("4", methods, label, "método de pago"));
        assertThrows(ValidationException.class,
                () -> InputValidator.parseChoice("Bitcoin", methods, label, "método de pago"));
    }

    @Test
    void yesNoAnswers() {
        assertTrue(InputValidator.parseYesNo("S"));
        assertTrue(InputValidator.parseYesNo("sí"));
        assertFalse(InputValidator.parseYesNo("no"));
        assertThrows(ValidationException.class, () -> InputValidator.parseYesNo("tal vez"));
    }
}
