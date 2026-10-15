package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.List;
import java.util.Objects;

/**
 * Main menu of the application.
 *
 * <p>Modules that are not implemented yet are labelled as pending and only
 * show a notice, so no option pretends to work before it exists.</p>
 */
public class MainMenuView {

    private final ConsoleUtils console;
    private final SystemMenuView systemMenuView;

    public MainMenuView(ConsoleUtils console, SystemMenuView systemMenuView) {
        this.console = Objects.requireNonNull(console, "console");
        this.systemMenuView = Objects.requireNonNull(systemMenuView, "systemMenuView");
    }

    /** Shows the main menu until the user chooses "Salir". */
    public void show() {
        Menu.main(console, List.of(
                        pending("Gestión de estudiantes y acudientes", 2),
                        pending("Gestión académica", 2),
                        pending("Gestión de profesores", 2),
                        pending("Actividades extracurriculares", 3),
                        pending("Gestión financiera", 4),
                        pending("Gestión de recursos", 5),
                        pending("Reportes y estadísticas", 6),
                        new Menu.Item("Configuración y estado del sistema", systemMenuView::show)),
                "ERP COLEGIO LITTLE HORIZONS",
                "Sistema de Gestión Educativa").run();
    }

    private Menu.Item pending(String moduleName, int phase) {
        return new Menu.Item(moduleName + " (pendiente)", () -> {
            console.printWarning("El módulo \"" + moduleName + "\" todavía no está disponible. "
                    + "Se implementará en la fase " + phase + " del proyecto.");
            console.pause();
        });
    }
}
