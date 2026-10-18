package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.List;
import java.util.Objects;

/**
 * Main menu of the application. Its options are supplied by
 * {@link com.littlehorizons.erp.ApplicationFactory}; a module that is not
 * available yet is added with {@link #pending} so it never pretends to work.
 */
public class MainMenuView {

    private final ConsoleUtils console;
    private final List<Menu.Item> items;

    public MainMenuView(ConsoleUtils console, List<Menu.Item> items) {
        this.console = Objects.requireNonNull(console, "console");
        this.items = List.copyOf(items);
    }

    /** Shows the main menu until the user chooses "Salir". */
    public void show() {
        Menu.main(console, items, "ERP COLEGIO LITTLE HORIZONS", "Sistema de Gestión Educativa").run();
    }

    /** Option for a module that is still under development. */
    public static Menu.Item pending(ConsoleUtils console, String moduleName, int phase) {
        return new Menu.Item(moduleName + " (pendiente)", () -> {
            console.printWarning("El módulo \"" + moduleName + "\" todavía no está disponible. "
                    + "Se implementará en la fase " + phase + " del proyecto.");
            console.pause();
        });
    }
}
