package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.InputUnavailableException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleUtilsTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private ConsoleUtils consoleWithInput(String input) {
        return new ConsoleUtils(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void invalidInputShowsMessageAndAsksAgain() {
        ConsoleUtils console = consoleWithInput("7\nabc\n4,5\n");

        BigDecimal grade = console.readGrade("Nota: ");

        assertEquals(new BigDecimal("4.5"), grade);
        String text = printed();
        assertTrue(text.contains("[ERROR] La nota debe estar entre 0.0 y 5.0."));
        assertTrue(text.contains("[ERROR] El campo nota debe ser un número válido"));
    }

    @Test
    void endOfInputRaisesInputUnavailable() {
        ConsoleUtils console = consoleWithInput("");
        assertThrows(InputUnavailableException.class, () -> console.readLine("> "));
    }

    @Test
    void accentedInputIsReadCorrectly() {
        ConsoleUtils console = consoleWithInput("José Ñúñez\n");
        assertEquals("José Ñúñez", console.readRequiredText("Nombre: ", "nombre", 100));
    }

    @Test
    void boxIsDrawnWithSameWidthOnEveryLine() {
        ConsoleUtils console = consoleWithInput("");
        console.printBox("ERP COLEGIO LITTLE HORIZONS", "Sistema de Gestión Educativa");

        List<String> lines = printed().lines().filter(line -> !line.isEmpty()).toList();
        assertEquals(4, lines.size());
        assertTrue(lines.get(0).startsWith("╔") && lines.get(3).startsWith("╚"));
        assertTrue(lines.stream().allMatch(line -> line.length() == lines.get(0).length()));
    }

    @Test
    void tableColumnsAreAligned() {
        ConsoleUtils console = consoleWithInput("");
        console.printTable(List.of("Dato", "Valor"), List.of(
                List.of("Servidor", "8.0.46"),
                List.of("Base de datos", "erp_little_horizons")));

        List<String> lines = printed().lines().toList();
        assertTrue(lines.stream().allMatch(line -> line.length() == lines.get(0).length()));
    }

    @Test
    void autoEncodingFallsBackToUtf8WithoutTerminal() {
        // Bajo Maven Surefire no hay una consola interactiva.
        if (System.console() == null) {
            assertEquals(StandardCharsets.UTF_8, ConsoleUtils.resolveCharset("AUTO"));
        }
        assertEquals(StandardCharsets.UTF_8, ConsoleUtils.resolveCharset("no-existe"));
        assertEquals("IBM850", ConsoleUtils.resolveCharset("IBM850").name());
    }
}
