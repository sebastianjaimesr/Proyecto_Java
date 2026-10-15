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
 * Central configuration of {@code java.util.logging} for the application.
 *
 * <p>Warnings and errors from every class under {@code com.littlehorizons.erp}
 * are appended to {@code <logs.dir>/error_log.txt}. Console output of log
 * records is disabled so that stack traces never reach the user.</p>
 *
 * <p>Never pass passwords or unnecessary personal data in log messages.</p>
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
     * Creates the logs directory if needed and attaches the file handler.
     * Calling it again has no effect while the logger is active.
     *
     * @throws ConfigurationException if the log file cannot be created
     */
    public static synchronized void init(Path logsDirectory) {
        if (fileHandler != null) {
            return;
        }
        Path directory = logsDirectory.toAbsolutePath().normalize();
        Path file = directory.resolve(ERROR_LOG_FILE);
        try {
            Files.createDirectories(directory);
            // '%' is a placeholder in FileHandler patterns, so it must be escaped.
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

    /** Logger for a class; its records go to {@code error_log.txt}. */
    public static Logger getLogger(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    /** Writes an error with its stack trace to {@code error_log.txt}. */
    public static void logError(Class<?> source, String message, Throwable error) {
        Logger logger = getLogger(source);
        if (logger.isLoggable(Level.SEVERE)) {
            logger.logp(Level.SEVERE, source.getName(), callerMethodName(), message, error);
        }
    }

    /** Absolute path of the active log file, or {@code null} before {@link #init(Path)}. */
    public static synchronized Path getLogFile() {
        return logFile;
    }

    /** Flushes and closes the log file; used when the application exits. */
    public static synchronized void shutdown() {
        if (fileHandler != null) {
            ROOT_LOGGER.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
            logFile = null;
        }
    }

    /** Name of the method that called {@link #logError}, for the log entry. */
    private static String callerMethodName() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .filter(frame -> !frame.getClassName().equals(AppLogger.class.getName()))
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse(null));
    }
}
