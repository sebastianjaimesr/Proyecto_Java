package com.littlehorizons.erp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Writes text files with {@link BufferedWriter} in UTF-8.
 *
 * <p>The parent folder is created when it does not exist. try-with-resources
 * flushes and closes the writer even when writing fails.</p>
 */
public final class TextFileWriter {

    /** Byte order mark: lets Excel recognise UTF-8 (tildes and ñ) in CSV files. */
    public static final String UTF8_BOM = "﻿";

    private TextFileWriter() {
    }

    /** Creates the file, or replaces its content, with the given lines. */
    public static Path write(Path file, List<String> lines) throws IOException {
        return writeLines(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    /** Adds the lines at the end of the file (append mode), creating it if needed. */
    public static Path append(Path file, List<String> lines) throws IOException {
        return writeLines(file, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);
    }

    private static Path writeLines(Path file, List<String> lines, OpenOption... options) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, options)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        return file.toAbsolutePath();
    }
}
