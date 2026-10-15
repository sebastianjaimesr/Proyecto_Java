package com.littlehorizons.erp.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppLoggerTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void closeLogger() {
        AppLogger.shutdown();
    }

    @Test
    void errorsAreWrittenToErrorLogWithAllFields() throws IOException {
        AppLogger.init(tempDir.resolve("logs"));
        Path logFile = AppLogger.getLogFile();
        assertEquals(AppLogger.ERROR_LOG_FILE, logFile.getFileName().toString());

        AppLogger.logError(AppLoggerTest.class, "Fallo de prueba con tildes: matrícula",
                new SQLException("Connection refused", "08S01"));
        AppLogger.shutdown();

        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(content.matches("(?s)^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}] SEVERE .*"),
                "entry must start with date and level");
        assertTrue(content.contains(AppLoggerTest.class.getName() + "#errorsAreWrittenToErrorLogWithAllFields"),
                "entry must contain class and method");
        assertTrue(content.contains("Message: Fallo de prueba con tildes: matrícula"), "message in UTF-8");
        assertTrue(content.contains("java.sql.SQLException: Connection refused"), "exception type");
        assertTrue(content.contains("\tat "), "stack trace lines");
    }

    @Test
    void entriesAreAppendedAcrossExecutions() throws IOException {
        Path logs = tempDir.resolve("logs");
        AppLogger.init(logs);
        AppLogger.logError(AppLoggerTest.class, "primero", new IllegalStateException("a"));
        AppLogger.shutdown();
        AppLogger.init(logs);
        AppLogger.logError(AppLoggerTest.class, "segundo", new IllegalStateException("b"));
        AppLogger.shutdown();

        String content = Files.readString(logs.resolve(AppLogger.ERROR_LOG_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Message: primero"));
        assertTrue(content.contains("Message: segundo"));
    }

    @Test
    void formatterWorksWithoutException() {
        LogRecord logRecord = new LogRecord(Level.WARNING, "Aviso sin excepción");
        logRecord.setLoggerName("com.littlehorizons.erp.Sample");
        logRecord.setInstant(Instant.EPOCH);

        String entry = new ErrorLogFormatter(ZoneId.of("UTC")).format(logRecord);

        assertTrue(entry.startsWith("[1970-01-01 00:00:00] WARNING com.littlehorizons.erp.Sample"));
        assertTrue(entry.contains("Message: Aviso sin excepción"));
    }
}
