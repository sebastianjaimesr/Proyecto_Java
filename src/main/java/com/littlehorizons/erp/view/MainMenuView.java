package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.List;
import java.util.Objects;

/**
 * Menú principal de la aplicación. Sus opciones las entrega
 * {@link com.littlehorizons.erp.ApplicationFactory}; un módulo que todavía no
 * está disponible se agrega con {@link #pending}, así nunca aparenta funcionar.
 */
public class MainMenuView {

    private final ConsoleUtils console;
    private final List<Menu.Item> items;

    public MainMenuView(ConsoleUtils console, List<Menu.Item> items) {
        this.console = Objects.requireNonNull(console, "console");
        this.items = List.copyOf(items);
    }

    /** Muestra el menú principal hasta que el usuario elige "Salir". */
    public void show() {
        Menu.main(console, items, "ERP COLEGIO LITTLE HORIZONS", "Sistema de Gestión Educativa").run();
    }

    /** Opción para un módulo que todavía está en desarrollo. */
    public static Menu.Item pending(ConsoleUtils console, String moduleName, int phase) {
        return new Menu.Item(moduleName + " (pendiente)", () -> {
            console.printWarning("El módulo \"" + moduleName + "\" todavía no está disponible. "
                    + "Se implementará en la fase " + phase + " del proyecto.");
            console.pause();
        });
    }
}
