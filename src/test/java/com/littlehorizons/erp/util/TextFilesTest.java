package com.littlehorizons.erp.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BufferedWriter / BufferedReader helpers of the text file exercises.
 */
class TextFilesTest {

    @TempDir
    Path folder;

    @Test
    void writeReplacesAndAppendKeepsPreviousLines() throws IOException {
        Path file = folder.resolve("sub/carpeta/bitacora.txt");

        TextFileWriter.write(file, List.of("uno", "dos"));
        TextFileWriter.write(file, List.of("nuevo"));
        TextFileWriter.append(file, List.of("agregado"));

        assertEquals(List.of("nuevo", "agregado"), Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    @Test
    void dataLinesSkipCommentsBlankLinesAndBomButKeepLineNumbers() throws IOException {
        Path file = folder.resolve("datos.txt");
        TextFileWriter.write(file, List.of(TextFileWriter.UTF8_BOM + "# comentario", "", "1010; I ;4,5",
                "   ", "Ñandú;Peña;"));

        List<TextFileReader.Line> lines = TextFileReader.readDataLines(file);

        assertEquals(2, lines.size());
        assertEquals(3, lines.get(0).number());
        assertArrayEquals(new String[] {"1010", "I", "4,5"}, lines.get(0).fields());
        assertEquals(5, lines.get(1).number());
        assertArrayEquals(new String[] {"Ñandú", "Peña", ""}, lines.get(1).fields());
    }

    @Test
    void readAllLinesRemovesOnlyTheBom() throws IOException {
        Path file = folder.resolve("todo.txt");
        TextFileWriter.write(file, List.of(TextFileWriter.UTF8_BOM + "Recibo;Valor", "", "# nota"));

        assertEquals(List.of("Recibo;Valor", "", "# nota"), TextFileReader.readAllLines(file));
    }

    @Test
    void safeFileNamesHaveNoAccentsSpacesOrSymbols() {
        assertEquals("robotica_junior_2", FileNames.safe("Robótica Junior #2"));
        assertEquals("1er_lugar", FileNames.safe("1er Lugar"));
        assertEquals("sin_nombre", FileNames.safe("  ¡! "));
    }

    @Test
    void scriptConsoleReadsAnswersFromAFileAndEchoesThem() throws IOException {
        Path script = folder.resolve("guion.txt");
        TextFileWriter.write(script, List.of("7", "José"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ConsoleUtils console = new ConsoleUtils(Files.newInputStream(script),
                new PrintStream(output, true, StandardCharsets.UTF_8), StandardCharsets.UTF_8, true)) {

            assertEquals(7, console.readInt("Opción: ", "opción", 1, 9));
            assertEquals("José", console.readLine("Nombre: "));
        }
        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Opción: 7"));
        assertTrue(printed.contains("Nombre: José"));
    }
}
