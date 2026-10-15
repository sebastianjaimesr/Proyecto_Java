package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reusable console menu: prints the header and the numbered options, reads a
 * valid choice and runs its action until the user picks the exit option.
 *
 * <p>Modules add their screens as {@link Item}s, so new options do not
 * require changes to this class (Open/Closed).</p>
 */
public final class Menu {

    /** One option of the menu. */
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

    /** Main menu: options 1..n and "Salir" as the last number. */
    public static Menu main(ConsoleUtils console, List<Item> items, String... header) {
        return new Menu(console, header, items, "Salir", true);
    }

    /** Submenu: options 1..n and "0. Volver al menú anterior". */
    public static Menu submenu(ConsoleUtils console, List<Item> items, String... header) {
        return new Menu(console, header, items, "Volver al menú anterior", false);
    }

    /** Shows the menu until the exit option is chosen. */
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
