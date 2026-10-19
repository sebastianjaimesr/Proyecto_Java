package com.littlehorizons.erp;

import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.InputUnavailableException;
import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.SystemMenuView;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada y raíz de composición: lee la configuración, inicia el registro
 * de errores y conecta vistas, controladores, servicios y DAOs a través de sus
 * constructores.
 */
public final class Main {

    private static final String DEFAULT_LOGS_DIR = "logs";

    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = null;
        ConfigurationException configError = null;
        try {
            config = AppConfig.getInstance();
        } catch (ConfigurationException e) {
            configError = e;
        }

        String logsDir = config != null ? config.getOptional("logs.dir", DEFAULT_LOGS_DIR) : DEFAULT_LOGS_DIR;
        String encoding = config != null
                ? config.getOptional("console.encoding", ConsoleUtils.AUTO_ENCODING)
                : ConsoleUtils.AUTO_ENCODING;

        ConsoleUtils console = ConsoleUtils.forSystemConsole(encoding);
        try {
            AppLogger.init(Paths.get(logsDir));
            Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                    AppLogger.logError(Main.class, "Uncaught exception in thread " + thread.getName(), error));
            if (configError != null) {
                throw configError;
            }
            run(config, console);
        } catch (ConfigurationException e) {
            AppLogger.logError(Main.class, "Configuration error at start-up", e);
            console.printError(e.getMessage());
        } catch (InputUnavailableException e) {
            console.println();
            console.printInfo("La entrada de la consola se cerró. La aplicación terminará.");
        } finally {
            console.printInfo("Recursos liberados. ¡Hasta pronto!");
            console.close();
            AppLogger.shutdown();
        }
    }

    private static void run(AppConfig config, ConsoleUtils console) {
        DatabaseConnection connection = DatabaseConnection.getInstance();
        Path logFile = AppLogger.getLogFile();
        ConfigurationSummary summary = new ConfigurationSummary(
                config.getSource(),
                connection.getSafeUrl(),
                connection.getUser(),
                logFile != null ? logFile.toString() : "(no disponible)",
                console.getCharset().displayName(),
                System.getProperty("java.version"));

        ApplicationFactory factory = new ApplicationFactory(config, console, connection, summary);
        SystemMenuView systemMenuView = factory.systemMenuView();
        MainMenuView mainMenuView = factory.mainMenuView(systemMenuView);

        console.printBox("ERP COLEGIO LITTLE HORIZONS", "Iniciando el sistema...");
        systemMenuView.printStartupStatus();
        mainMenuView.show();
    }
}
