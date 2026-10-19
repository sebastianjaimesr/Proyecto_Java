package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Menú de consola reutilizable: imprime el encabezado y las opciones numeradas, lee una
 * opción válida y ejecuta su acción hasta que el usuario elige la opción de salida.
 *
 * <p>Los módulos agregan sus pantallas como {@link Item}, así las opciones nuevas no
 * requieren cambiar esta clase (Abierto/Cerrado).</p>
 */
public final class Menu {

    /** Una opción del menú. */
    public record Item(String label, Runnable action) {

        public Item {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(action, "action");
        }
    }

    private static final String PROMPT = "Seleccione una opción: ";

    private final ConsoleUtils console;
    private final String[] header;
    private final List<Item> items;
    private final String exitLabel;
    private final boolean exitIsLastNumber;

    private Menu(ConsoleUtils console, String[] header, List<Item> items,
                 String exitLabel, boolean exitIsLastNumber) {
        this.console = Objects.requireNonNull(console, "console");
        this.header = header.clone();
        this.items = List.copyOf(items);
        this.exitLabel = exitLabel;
        this.exitIsLastNumber = exitIsLastNumber;
    }

    /** Menú principal: opciones 1..n y "Salir" como último número. */
    public static Menu main(ConsoleUtils console, List<Item> items, String... header) {
        return new Menu(console, header, items, "Salir", true);
    }

    /** Submenú: opciones 1..n y "0. Volver al menú anterior". */
    public static Menu submenu(ConsoleUtils console, List<Item> items, String... header) {
        return new Menu(console, header, items, "Volver al menú anterior", false);
    }

    /** Muestra el menú hasta que se elige la opción de salida. */
    public void run() {
        int exitNumber = exitIsLastNumber ? items.size() + 1 : 0;
        int min = exitIsLastNumber ? 1 : 0;
        int max = exitIsLastNumber ? exitNumber : items.size();
        while (true) {
            console.printBox(header);
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                lines.add((i + 1) + ". " + items.get(i).label());
            }
            lines.add(exitNumber + ". " + exitLabel);
            lines.forEach(console::println);
            console.println();
            int option = console.readMenuOption(PROMPT, min, max);
            if (option == exitNumber) {
                return;
            }
            items.get(option - 1).action().run();
        }
    }
}
