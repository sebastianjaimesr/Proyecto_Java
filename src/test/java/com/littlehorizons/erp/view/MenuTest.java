package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Navigation between the main menu and nested submenus.
 */
class MenuTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private ConsoleUtils consoleWithInput(String input) {
        return new ConsoleUtils(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    void mainMenuOptionLeavesEveryNestedSubmenu() {
        // Main → 1 (Módulo) → 1 (Submódulo) → 2 "Volver al menú principal" → main again → 1 → 0 → 2 (Salir)
        ConsoleUtils console = consoleWithInput("1\n1\n2\n1\n0\n2\n");
        List<String> visits = new ArrayList<>();
        Menu leaf = Menu.submenu(console, List.of(new Menu.Item("Acción", () -> visits.add("action"))), "SUBMÓDULO");
        Menu module = Menu.submenu(console, List.of(new Menu.Item("Submódulo", () -> {
            visits.add("leaf");
            leaf.run();
        })), "MÓDULO");
        Menu.main(console, List.of(new Menu.Item("Módulo", () -> {
            visits.add("module");
            module.run();
        })), "PRINCIPAL").run();

        assertEquals(List.of("module", "leaf", "module"), visits);
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("2. Volver al menú principal"));
        assertTrue(text.contains("0. Volver al menú anterior"));
        assertTrue(text.contains("2. Salir"));
    }
}
