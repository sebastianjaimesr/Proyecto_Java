package com.littlehorizons.erp.service;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.common.DbEnum;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.TextFileReader;
import com.littlehorizons.erp.util.TextFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * File operations for the services: the {@link IOException} goes to
 * error_log.txt with its stack trace and the user gets a message in Spanish.
 */
final class FileSupport {

    /**
     * Root folder of every generated report, relative to the project folder.
     * The tests point it to target/ with the system property erp.exports.dir.
     */
    static final Path EXPORTS = Path.of(System.getProperty("erp.exports.dir", "exports"));

    private FileSupport() {
    }

    /** Folder of error_log.txt (logs.dir), also used for the journals. */
    static Path logsFolder() {
        Path logFile = AppLogger.getLogFile();
        return logFile != null && logFile.getParent() != null
                ? logFile.getParent() : Path.of(System.getProperty("erp.logs.dir", "logs"));
    }

    static Path write(Path file, List<String> lines, Class<?> source) {
        try {
            return TextFileWriter.write(file, lines);
        } catch (IOException e) {
            AppLogger.logError(source, "Could not write " + file.toAbsolutePath(), e);
            throw new BusinessRuleException("No fue posible crear el archivo " + file.getFileName()
                    + ". El detalle técnico quedó en error_log.txt.");
        }
    }

    static Path append(Path file, List<String> lines, Class<?> source) {
        try {
            return TextFileWriter.append(file, lines);
        } catch (IOException e) {
            AppLogger.logError(source, "Could not append to " + file.toAbsolutePath(), e);
            throw new BusinessRuleException("No fue posible escribir en el archivo " + file.getFileName()
                    + ". El detalle técnico quedó en error_log.txt.");
        }
    }

    /**
     * Appends without failing: used for journals, which must never undo the
     * operation they record.
     */
    static void appendQuietly(Path file, List<String> lines, Class<?> source) {
        try {
            TextFileWriter.append(file, lines);
        } catch (IOException e) {
            AppLogger.logError(source, "Could not append to journal " + file.toAbsolutePath(), e);
        }
    }

    static List<TextFileReader.Line> readDataLines(Path file, Class<?> source) {
        requireFile(file);
        try {
            return TextFileReader.readDataLines(file);
        } catch (NoSuchFileException e) {
            throw notFound(file);
        } catch (IOException e) {
            AppLogger.logError(source, "Could not read " + file.toAbsolutePath(), e);
            throw new BusinessRuleException("No fue posible leer el archivo " + file.getFileName()
                    + ". El detalle técnico quedó en error_log.txt.");
        }
    }

    static List<String> readAllLines(Path file, Class<?> source) {
        requireFile(file);
        try {
            return TextFileReader.readAllLines(file);
        } catch (NoSuchFileException e) {
            throw notFound(file);
        } catch (IOException e) {
            AppLogger.logError(source, "Could not read " + file.toAbsolutePath(), e);
            throw new BusinessRuleException("No fue posible leer el archivo " + file.getFileName()
                    + ". El detalle técnico quedó en error_log.txt.");
        }
    }

    static void requireFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw notFound(file);
        }
    }

    /**
     * One CSV row for Excel in Spanish: ";" as separator (the comma is the
     * decimal mark there); cells with ";", quotes or line breaks are quoted.
     */
    static String csvRow(List<String> cells) {
        return cells.stream().map(FileSupport::csvCell).collect(Collectors.joining(TextFileReader.SEPARATOR));
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.contains(TextFileReader.SEPARATOR) || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    /** Cuts long texts so fixed-width columns stay aligned. */
    static String fit(String text, int width) {
        if (text == null) {
            return "-";
        }
        return text.length() <= width ? text : text.substring(0, width - 1) + "…";
    }

    /** "Línea 4: message", the format of every import error. */
    static String lineError(int lineNumber, String message) {
        return "Línea " + lineNumber + ": " + message;
    }

    /** Enum constant by its stored value, ignoring case ("ti", "Pasaporte", "f"). */
    static <E extends Enum<E> & DbEnum> E parseEnum(String raw, Class<E> type, String fieldName) {
        String value = raw == null ? "" : raw.strip();
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.dbValue().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException(fieldName + " no válido: '" + value + "'. Valores: "
                        + Arrays.stream(type.getEnumConstants()).map(DbEnum::dbValue)
                        .collect(Collectors.joining(", ")) + "."));
    }

    private static ValidationException notFound(Path file) {
        return new ValidationException("El archivo no existe: " + file.toAbsolutePath());
    }
}
