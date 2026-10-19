package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.controller.SystemController;
import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.model.system.DatabaseInfo;
import com.littlehorizons.erp.model.system.SchemaStatus;
import com.littlehorizons.erp.util.ConsoleUtils;

import java.util.List;
import java.util.Objects;

/**
 * Pantallas de "8. Configuración y estado del sistema".
 */
public class SystemMenuView {

    private final ConsoleUtils console;
    private final SystemController controller;

    public SystemMenuView(ConsoleUtils console, SystemController controller) {
        this.console = Objects.requireNonNull(console, "console");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /** Abre el submenú. */
    public void show() {
        Menu.submenu(console, List.of(
                        new Menu.Item("Probar la conexión y verificar el esquema", this::showDatabaseStatus),
                        new Menu.Item("Ver la configuración activa", this::showConfiguration)),
                "CONFIGURACIÓN Y ESTADO DEL SISTEMA").run();
    }

    /**
     * Imprime en una línea el estado de la conexión; se usa al iniciar.
     *
     * @return si la base de datos responde y está completa
     */
    public boolean printStartupStatus() {
        OperationResult<SchemaStatus> result = controller.checkDatabase();
        if (!result.success()) {
            console.printWarning("Base de datos no disponible. " + result.message());
            console.printInfo("Puede revisar la conexión en la opción 8 del menú principal.");
            return false;
        }
        SchemaStatus status = result.data();
        DatabaseInfo info = status.databaseInfo();
        if (status.isComplete()) {
            console.printSuccess("Conectado a MySQL " + info.serverVersion()
                    + " · base de datos " + info.databaseName() + ".");
        } else {
            console.printWarning(result.message() + " Revise la opción 8 del menú principal.");
        }
        return status.isComplete();
    }

    private void showDatabaseStatus() {
        console.printTitle("Prueba de conexión");
        OperationResult<SchemaStatus> result = controller.checkDatabase();
        if (!result.success()) {
            console.printError(result.message());
            console.pause();
            return;
        }
        SchemaStatus status = result.data();
        DatabaseInfo info = status.databaseInfo();
        console.printTable(List.of("Dato", "Valor"), List.of(
                List.of("Servidor MySQL", info.serverVersion()),
                List.of("Base de datos", info.databaseName()),
                List.of("Tablas en la base", String.valueOf(info.tableNames().size())),
                List.of("Tablas oficiales encontradas",
                        status.foundOfficialTables() + " de " + status.expectedTables())));
        if (status.isComplete()) {
            console.printSuccess(result.message());
        } else {
            console.printWarning(result.message());
            console.println("Tablas faltantes: " + String.join(", ", status.missingTables()));
            console.println("Ejecute el script oficial de la base de datos para crearlas.");
        }
        console.pause();
    }

    private void showConfiguration() {
        console.printTitle("Configuración activa");
        OperationResult<ConfigurationSummary> result = controller.getConfiguration();
        if (!result.success()) {
            console.printError(result.message());
            console.pause();
            return;
        }
        ConfigurationSummary summary = result.data();
        console.printTable(List.of("Parámetro", "Valor"), List.of(
                List.of("Archivo de configuración", summary.configFile()),
                List.of("URL JDBC", summary.jdbcUrl()),
                List.of("Usuario MySQL", summary.databaseUser()),
                List.of("Contraseña", "(oculta)"),
                List.of("Registro de errores", summary.errorLogFile()),
                List.of("Codificación de consola", summary.consoleEncoding()),
                List.of("Versión de Java", summary.javaVersion())));
        console.pause();
    }
}
