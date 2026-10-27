package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.InputUnavailableException;
import com.littlehorizons.erp.exception.ValidationException;

import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Console input and output helpers shared by every view.
 *
 * <p>Reading methods keep asking until the value passes the matching
 * {@link InputValidator} rule, printing the validation message in Spanish
 * after each invalid attempt.</p>
 *
 * <p>Exercise 21: every line typed in the program is read here, with a
 * {@link Scanner} (it was a BufferedReader). No view knows how input is read,
 * so this was the only class that changed. The input can also be a script
 * file instead of the keyboard (see {@link #forScript}).</p>
 */
public class ConsoleUtils implements AutoCloseable {

    /** Value of {@code console.encoding} that picks the encoding automatically. */
    public static final String AUTO_ENCODING = "AUTO";

    private static final int MIN_BOX_WIDTH = 46;

    private final Scanner scanner;
    private final PrintStream out;
    private final Charset charset;
    private final boolean echoInput;

    public ConsoleUtils(InputStream in, PrintStream out, Charset charset) {
        this(in, out, charset, false);
    }

    /**
     * @param echoInput prints each line read after its prompt; used with
     *                  scripts, where nobody types and the answers would not
     *                  show otherwise
     */
    public ConsoleUtils(InputStream in, PrintStream out, Charset charset, boolean echoInput) {
        this.scanner = new Scanner(in, charset);
        this.out = out;
        this.charset = charset;
        this.echoInput = echoInput;
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
     * Exercise 21 (b): console that reads its answers from a UTF-8 script file,
     * one answer per line, and writes to the standard output. When the script
     * ends, {@link #readLine} throws {@link InputUnavailableException} and the
     * program closes as it does when the keyboard input ends.
     */
    public static ConsoleUtils forScript(Path script, String encodingSetting) throws IOException {
        Charset outputCharset = resolveCharset(encodingSetting);
        PrintStream stdout = new PrintStream(new FileOutputStream(FileDescriptor.out), true, outputCharset);
        return new ConsoleUtils(Files.newInputStream(script), stdout, StandardCharsets.UTF_8, true);
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
        if (!scanner.hasNextLine()) { // end of input: Ctrl+Z / Ctrl+D or end of the script
            throw new InputUnavailableException();
        }
        String line = scanner.nextLine();
        if (echoInput) {
            out.println(line);
        }
        return line;
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

    /** Releases the input scanner; called once when the application exits. */
    @Override
    public void close() {
        out.flush();
        IOException error = scanner.ioException();
        if (error != null) {
            AppLogger.logError(ConsoleUtils.class, "Console input failed", error);
        }
        scanner.close();
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
