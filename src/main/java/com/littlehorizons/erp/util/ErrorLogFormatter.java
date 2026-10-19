package com.littlehorizons.erp.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Da formato a cada entrada de {@code error_log.txt} con fecha, nivel, clase, método,
 * mensaje y stack trace completo.
 *
 * <pre>
 * [2026-10-15 14:03:22] SEVERE com.littlehorizons.erp.dao.jdbc.JdbcSystemDao#findStatus
 * Message: Could not read database status
 * java.sql.SQLException: ...
 * at ...
 * ----------------------------------------------------------------------
 * </pre>
 */
public class ErrorLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "-".repeat(70);

    private final ZoneId zoneId;

    public ErrorLogFormatter() {
        this(ZoneId.systemDefault());
    }

    ErrorLogFormatter(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public String format(LogRecord logRecord) {
        StringBuilder entry = new StringBuilder(256);
        entry.append('[')
                .append(TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(logRecord.getMillis()).atZone(zoneId)))
                .append("] ")
                .append(logRecord.getLevel().getName())
                .append(' ')
                .append(sourceOf(logRecord))
                .append(System.lineSeparator())
                .append("Message: ")
                .append(formatMessage(logRecord))
                .append(System.lineSeparator());

        Throwable thrown = logRecord.getThrown();
        if (thrown != null) {
            StringWriter stackTrace = new StringWriter();
            try (PrintWriter writer = new PrintWriter(stackTrace)) {
                thrown.printStackTrace(writer);
            }
            entry.append(stackTrace);
        }
        entry.append(SEPARATOR).append(System.lineSeparator());
        return entry.toString();
    }

    /** Clase y método que generaron el registro, en formato Clase#metodo. */
    private static String sourceOf(LogRecord logRecord) {
        String className = logRecord.getSourceClassName() != null
                ? logRecord.getSourceClassName()
                : logRecord.getLoggerName();
        return logRecord.getSourceMethodName() != null
                ? className + "#" + logRecord.getSourceMethodName()
                : className;
    }
}
