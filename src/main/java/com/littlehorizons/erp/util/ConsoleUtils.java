package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.InputUnavailableException;
import com.littlehorizons.erp.exception.ValidationException;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Console input and output helpers shared by every view.
 *
 * <p>Reading methods keep asking until the value passes the matching
 * {@link InputValidator} rule, printing the validation message in Spanish
 * after each invalid attempt.</p>
 */
public class ConsoleUtils implements AutoCloseable {

    /** Value of {@code console.encoding} that picks the encoding automatically. */
    public static final String AUTO_ENCODING = "AUTO";

    private static final int MIN_BOX_WIDTH = 46;

    private final BufferedReader reader;
    private final PrintStream out;
    private final Charset charset;

    public ConsoleUtils(InputStream in, PrintStream out, Charset charset) {
        this.reader = new BufferedReader(new InputStreamReader(in, charset));
        this.out = out;
        this.charset = charset;
    }

    /**
     * Console bound to the process standard streams.
     *
     * @param encodingSetting {@value #AUTO_ENCODING} or a charset name such as UTF-8
     */
    public static ConsoleUtils forSystemConsole(String encodingSetting) {
        Charset charset = resolveCharset(encodingSetting);
        PrintStream stdout = new PrintStream(new FileOutputStream(FileDescriptor.out), true, charset);
        return new ConsoleUtils(System.in, stdout, charset);
    }

    /**
     * AUTO uses the terminal encoding when the program runs in a real console
     * (for example cmd with code page 850) and UTF-8 otherwise (NetBeans output
     * window, redirected streams). Unknown names fall back to UTF-8.
     */
    static Charset resolveCharset(String encodingSetting) {
        if (encodingSetting == null || encodingSetting.isBlank()
                || AUTO_ENCODING.equalsIgnoreCase(encodingSetting.trim())) {
            Console console = System.console();
            return console != null ? console.charset() : StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encodingSetting.trim());
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            return StandardCharsets.UTF_8;
        }
    }

    public Charset getCharset() {
        return charset;
    }

    // ------------------------------------------------------------------ output

    /** Title framed in a double-line box. */
    public void printBox(String... lines) {
        int width = Math.max(MIN_BOX_WIDTH,
                IntStream.range(0, lines.length).map(i -> lines[i].length()).max().orElse(0) + 4);
        out.println();
        out.println("╔" + "═".repeat(width) + "╗");
        for (String line : lines) {
            int padding = width - line.length();
            int left = padding / 2;
            out.println("║" + " ".repeat(left) + line + " ".repeat(padding - left) + "║");
        }
        out.println("╚" + "═".repeat(width) + "╝");
    }

    /** Section title with an underline. */
    public void printTitle(String title) {
        out.println();
        out.println(title);
        out.println("─".repeat(title.length()));
    }

    public void println(String text) {
        out.println(text);
    }

    public void println() {
        out.println();
    }

    public void printSuccess(String message) {
        out.println("[OK] " + message);
    }

    public void printError(String message) {
        out.println("[ERROR] " + message);
    }

    public void printWarning(String message) {
        out.println("[AVISO] " + message);
    }

    public void printInfo(String message) {
        out.println("[INFO] " + message);
    }

    /** Numbered menu: each option as "n. label". */
    public void printMenu(List<String> options) {
        IntStream.range(0, options.size())
                .forEach(i -> out.println((i + 1) + ". " + options.get(i)));
    }

    /** Simple text table with columns sized to their widest value. */
    public void printTable(List<String> headers, List<List<String>> rows) {
        int[] widths = IntStream.range(0, headers.size())
                .map(column -> Math.max(headers.get(column).length(),
                        rows.stream()
                                .mapToInt(row -> valueAt(row, column).length())
                                .max()
                                .orElse(0)))
                .toArray();
        String separator = IntStream.range(0, widths.length)
                .mapToObj(column -> "-".repeat(widths[column] + 2))
                .collect(Collectors.joining("+", "+", "+"));
        out.println(separator);
        out.println(formatRow(headers, widths));
        out.println(separator);
        rows.forEach(row -> out.println(formatRow(row, widths)));
        out.println(separator);
    }

    // ------------------------------------------------------------------- input

    /**
     * Reads one line after showing the prompt.
     *
     * @throws InputUnavailableException if the input stream has ended
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new InputUnavailableException();
            }
            return line;
        } catch (IOException e) {
            throw new InputUnavailableException();
        }
    }

    /** Asks again until {@code parser} accepts the text. */
    public <T> T readValid(String prompt, Function<String, T> parser) {
        while (true) {
            String line = readLine(prompt);
            try {
                return parser.apply(line);
            } catch (ValidationException e) {
                printError(e.getMessage());
            }
        }
    }

    public int readMenuOption(String prompt, int min, int max) {
        return readValid(prompt, raw -> InputValidator.parseMenuOption(raw, min, max));
    }

    public String readRequiredText(String prompt, String fieldName, int maxLength) {
        return readValid(prompt, raw -> InputValidator.requireText(raw, fieldName, maxLength));
    }

    public String readOptionalText(String prompt, String fieldName, int maxLength) {
        return readValid(prompt, raw -> InputValidator.optionalText(raw, fieldName, maxLength));
    }

    public int readInt(String prompt, String fieldName, int min, int max) {
        return readValid(prompt, raw -> InputValidator.parseInt(raw, fieldName, min, max));
    }

    public int readId(String prompt, String fieldName) {
        return readValid(prompt, raw -> InputValidator.parseId(raw, fieldName));
    }

    public BigDecimal readGrade(String prompt) {
        return readValid(prompt, InputValidator::parseGrade);
    }

    public BigDecimal readMoney(String prompt, String fieldName) {
        return readValid(prompt, raw -> InputValidator.parseMoney(raw, fieldName));
    }

    public LocalDate readDate(String prompt, String fieldName) {
        return readValid(prompt, raw -> InputValidator.parseDate(raw, fieldName));
    }

    public String readEmail(String prompt, String fieldName) {
        return readValid(prompt, raw -> InputValidator.validateEmail(raw, fieldName));
    }

    public String readPhone(String prompt, String fieldName) {
        return readValid(prompt, raw -> InputValidator.validatePhone(raw, fieldName));
    }

    public String readDocumentNumber(String prompt) {
        return readValid(prompt, InputValidator::validateDocumentNumber);
    }

    public String readCode(String prompt, String fieldName, int maxLength) {
        return readValid(prompt, raw -> InputValidator.validateCode(raw, fieldName, maxLength));
    }

    /** Shows the numbered options and returns the chosen one. */
    public <T> T readChoice(String prompt, List<T> options, Function<T, String> labelOf, String fieldName) {
        printMenu(options.stream().map(labelOf).collect(Collectors.toList()));
        return readValid(prompt, raw -> InputValidator.parseChoice(raw, options, labelOf, fieldName));
    }

    public boolean confirm(String question) {
        return readValid(question + " (S/N): ", InputValidator::parseYesNo);
    }

    public void pause() {
        readLine("Presione Enter para continuar...");
    }

    /** Releases the input reader; called once when the application exits. */
    @Override
    public void close() {
        out.flush();
        try {
            reader.close();
        } catch (IOException e) {
            AppLogger.logError(ConsoleUtils.class, "Could not close console input", e);
        }
    }

    private static String formatRow(List<String> values, int[] widths) {
        return IntStream.range(0, widths.length)
                .mapToObj(column -> " " + padRight(valueAt(values, column), widths[column]) + " ")
                .collect(Collectors.joining("|", "|", "|"));
    }

    private static String valueAt(List<String> row, int column) {
        if (column >= row.size() || row.get(column) == null) {
            return "";
        }
        return row.get(column);
    }

    private static String padRight(String value, int width) {
        return value + " ".repeat(width - value.length());
    }
}
