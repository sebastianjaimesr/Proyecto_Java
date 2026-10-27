package test.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicTextFileWriterTest {

    @TempDir
    Path folder;

    private final AtomicTextFileWriter writer = new AtomicTextFileWriter();

    @Test
    void writesUtf8AndReplacesAnExistingFile() throws IOException {
        Path file = folder.resolve("materias_profesor_1_2025.txt");
        Files.writeString(file, "contenido anterior mucho más largo que el nuevo", StandardCharsets.UTF_8);

        writer.write(file, List.of("AÑO 2025", "Matemáticas · Décimo"));

        assertEquals(List.of("AÑO 2025", "Matemáticas · Décimo"), Files.readAllLines(file, StandardCharsets.UTF_8));
        assertEquals(List.of(file), filesIn(folder));
    }

    @Test
    void failureIsReportedAndLeavesNoTemporaryFiles() throws IOException {
        Path blocked = folder.resolve("materias_profesor_1_2025.txt");
        Files.createDirectories(blocked.resolve("ocupado"));

        assertThrows(IOException.class, () -> writer.write(blocked, List.of("dato")));
        assertEquals(List.of(blocked), filesIn(folder));
    }

    private static List<Path> filesIn(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.toList();
        }
    }
}
