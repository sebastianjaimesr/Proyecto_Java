package com.littlehorizons.erp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads text files with {@link BufferedReader} in UTF-8.
 *
 * <p>Data files used by the import options have one record per line with
 * fields separated by ";". Blank lines and lines starting with "#" are
 * comments and are skipped.</p>
 */
public final class TextFileReader {

    public static final String SEPARATOR = ";";

    /** One data line and its number in the file, for error messages. */
    public record Line(int number, String text) {

        /** Fields separated by ";", trimmed. Empty fields are kept as "". */
        public String[] fields() {
            String[] parts = text.split(SEPARATOR, -1);
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].strip();
            }
            return parts;
        }
    }

    private TextFileReader() {
    }

    /** Every line of the file, as written (only a leading BOM is removed). */
    public static List<String> readAllLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) { // null means end of file
                lines.add(lines.isEmpty() ? removeBom(line) : line);
            }
        }
        return lines;
    }

    /** Data lines only: skips blank lines and "#" comments, keeping the original line numbers. */
    public static List<Line> readDataLines(Path file) throws IOException {
        List<Line> data = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String text;
            int number = 0;
            while ((text = reader.readLine()) != null) {
                number++;
                text = (number == 1 ? removeBom(text) : text).strip();
                if (!text.isEmpty() && !text.startsWith("#")) {
                    data.add(new Line(number, text));
                }
            }
        }
        return data;
    }

    private static String removeBom(String line) {
        return line.startsWith(TextFileWriter.UTF8_BOM) ? line.substring(1) : line;
    }
}
