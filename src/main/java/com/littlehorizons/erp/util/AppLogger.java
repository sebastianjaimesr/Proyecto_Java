package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ConfigurationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración central de {@code java.util.logging} para la aplicación.
 *
 * <p>Las advertencias y errores de todas las clases bajo {@code com.littlehorizons.erp}
 * se agregan a {@code <logs.dir>/error_log.txt}. La salida de registros por consola
 * está desactivada para que los stack traces nunca lleguen al usuario.</p>
 *
 * <p>Nunca pases contraseñas ni datos personales innecesarios en los mensajes de registro.</p>
 */
public final class AppLogger {

    public static final String ROOT_LOGGER_NAME = "com.littlehorizons.erp";
    public static final String ERROR_LOG_FILE = "error_log.txt";

    private static final Logger ROOT_LOGGER = Logger.getLogger(ROOT_LOGGER_NAME);

    private static FileHandler fileHandler;
    private static Path logFile;

    private AppLogger() {
    }

    /**
     * Crea la carpeta de logs si hace falta y conecta el manejador de archivo.
     * Llamarlo otra vez no tiene efecto mientras el logger esté activo.
     *
     * @throws ConfigurationException si no se puede crear el archivo de registro
     */
    public static synchronized void init(Path logsDirectory) {
        if (fileHandler != null) {
            return;
        }
        Path directory = logsDirectory.toAbsolutePath().normalize();
        Path file = directory.resolve(ERROR_LOG_FILE);
        try {
            Files.createDirectories(directory);
            // '%' es un marcador en los patrones de FileHandler, así que hay que escaparlo.
            FileHandler handler = new FileHandler(file.toString().replace("%", "%%"), true);
            handler.setEncoding(StandardCharsets.UTF_8.name());
            handler.setFormatter(new ErrorLogFormatter());
            handler.setLevel(Level.WARNING);
            ROOT_LOGGER.setUseParentHandlers(false);
            ROOT_LOGGER.setLevel(Level.INFO);
            ROOT_LOGGER.addHandler(handler);
            fileHandler = handler;
            logFile = file;
        } catch (IOException | SecurityException e) {
            throw new ConfigurationException("No se pudo crear el archivo de registro "
                    + file + ". Revise la propiedad logs.dir y los permisos de la carpeta.", e);
        }
    }

    /** Logger de una clase; sus registros van a {@code error_log.txt}. */
    public static Logger getLogger(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    /** Escribe un error con su stack trace en {@code error_log.txt}. */
    public static void logError(Class<?> source, String message, Throwable error) {
        Logger logger = getLogger(source);
        if (logger.isLoggable(Level.SEVERE)) {
            logger.logp(Level.SEVERE, source.getName(), callerMethodName(), message, error);
        }
    }

    /** Ruta absoluta del archivo de registro activo, o {@code null} antes de {@link #init(Path)}. */
    public static synchronized Path getLogFile() {
        return logFile;
    }

    /** Vacía y cierra el archivo de registro; se usa al salir de la aplicación. */
    public static synchronized void shutdown() {
        if (fileHandler != null) {
            ROOT_LOGGER.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
            logFile = null;
        }
    }

    /** Nombre del método que llamó a {@link #logError}, para la entrada del registro. */
    private static String callerMethodName() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .filter(frame -> !frame.getClassName().equals(AppLogger.class.getName()))
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse(null));
    }
}
