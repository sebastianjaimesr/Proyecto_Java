package test.controller;

import com.littlehorizons.erp.dao.SqlWork;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.util.ConsoleUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.model.AssignedSubject;
import test.model.TeacherSummary;
import test.view.SubjectExportView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectExportControllerTest {

    private static final String PROMPT = "Ingrese el ID del profesor: ";
    private static final Clock CLOCK = Clock.fixed(
            LocalDate.of(2026, 10, 27).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @TempDir
    Path folder;

    private final Map<Integer, TeacherSummary> teachers = new HashMap<>();
    private final Map<Integer, List<AssignedSubject>> subjects2025 = new HashMap<>();
    private final List<Integer> queriedIds = new ArrayList<>();

    @Test
    void invalidInputsAreRejectedInOrderUntilAValidTeacherIsFound() throws IOException {
        addTeacher(1, new PersonName("Roberto", "Andrés", "Martínez", "Torres"),
                new AssignedSubject("MAT-10", "Matemáticas", "Décimo"));

        List<String> output = run("abc\n\n12.5\n1e3\n٣\n99\n0\n-4\n99999999999\n007\n1\n");

        assertEquals(List.of(
                "EXPORTADOR DE MATERIAS ASIGNADAS - AÑO 2025",
                PROMPT + "Error: Debe ingresar un número válido.",
                PROMPT + "Error: Debe ingresar un número válido.",
                PROMPT + "Error: Debe ingresar un número válido.",
                PROMPT + "Error: Debe ingresar un número válido.",
                PROMPT + "Error: Debe ingresar un número válido.",
                PROMPT + "Error: No se encontró ningún profesor con el ID 99.",
                PROMPT + "Error: No se encontró ningún profesor con el ID 0.",
                PROMPT + "Error: No se encontró ningún profesor con el ID -4.",
                PROMPT + "Error: No se encontró ningún profesor con el ID 99999999999.",
                PROMPT + "Error: No se encontró ningún profesor con el ID 7.",
                PROMPT + "Archivo generado exitosamente",
                "Profesor: Roberto Andrés Martínez Torres",
                "Archivo: materias_profesor_1_2025.txt"), output);
        assertEquals(List.of(99, 7, 1), queriedIds);
    }

    @Test
    void successfulExportWritesTheFileInTheOutputFolder() throws IOException {
        addTeacher(1, new PersonName("Roberto", "Andrés", "Martínez", "Torres"),
                new AssignedSubject("MAT-10", "Matemáticas", "Décimo"),
                new AssignedSubject("MAT-11", "Matemáticas", "Once"));

        run(" 1 \n");

        List<String> file = Files.readAllLines(folder.resolve("materias_profesor_1_2025.txt"), StandardCharsets.UTF_8);
        assertEquals("REPORTE DE MATERIAS ASIGNADAS - AÑO 2025", file.get(1));
        assertEquals("Nombre completo: Roberto Andrés Martínez Torres", file.get(3));
        assertEquals("Documento: CC 80123456", file.get(4));
        assertEquals("MAT-11    Matemáticas                        Once                ", file.get(8));
        assertEquals("Documento generado el: 27/10/2026", file.get(file.size() - 1));
    }

    @Test
    void teacherWithoutSubjectsIn2025EndsWithoutCreatingTheFile() throws IOException {
        addTeacher(2, new PersonName("Felipe", null, "Sánchez", "Díaz"));

        List<String> output = run("2\n");

        assertEquals(PROMPT + "Error: El profesor Felipe Sánchez Díaz no tiene materias asignadas en 2025.",
                output.get(1));
        assertEquals(2, output.size());
        assertFalse(Files.exists(folder.resolve("materias_profesor_2_2025.txt")));
    }

    @Test
    void fileThatCannotBeWrittenShowsTheRequiredMessage() throws IOException {
        addTeacher(1, new PersonName("Ana", null, "Ruiz", null), new AssignedSubject("MAT-10", "Matemáticas", "Décimo"));
        Files.createDirectories(folder.resolve("materias_profesor_1_2025.txt").resolve("ocupado"));

        List<String> output = run("1\n");

        assertEquals(PROMPT + "Error: No se pudo crear el archivo de exportación.", output.get(1));
        assertEquals(2, output.size());
    }

    @Test
    void databaseFailureShowsAFriendlyMessage() throws IOException {
        TransactionManager unavailable = new TransactionManager() {
            @Override
            public <T> T inTransaction(SqlWork<T> work) {
                return read(work);
            }

            @Override
            public <T> T read(SqlWork<T> work) {
                throw new DataAccessException("Connection refused", new SQLException("down", "08S01"));
            }
        };

        List<String> output = run("1\n", unavailable);

        assertTrue(output.get(1).startsWith(PROMPT + "Error: No se pudo consultar la base de datos."));
        assertEquals(2, output.size());
    }

    @Test
    void closedInputEndsTheProgramInAnOrderlyWay() throws IOException {
        List<String> output = run("abc\n");

        assertEquals(List.of(
                "EXPORTADOR DE MATERIAS ASIGNADAS - AÑO 2025",
                PROMPT + "Error: Debe ingresar un número válido.",
                PROMPT,
                "Se cerró la entrada del teclado. El programa terminará."), output);
    }

    private void addTeacher(int id, PersonName name, AssignedSubject... subjects) {
        teachers.put(id, new TeacherSummary(id, PersonDocumentType.CC, "80123456", name));
        subjects2025.put(id, List.of(subjects));
    }

    private List<String> run(String input) throws IOException {
        return run(input, new DirectTransactions());
    }

    private List<String> run(String input, TransactionManager transactions) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ConsoleUtils console = new ConsoleUtils(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), StandardCharsets.UTF_8)) {
            SubjectExportController controller = new SubjectExportController(
                    transactions,
                    (connection, id) -> {
                        queriedIds.add(id);
                        return Optional.ofNullable(teachers.get(id));
                    },
                    (connection, id, year) -> year == 2025 ? subjects2025.getOrDefault(id, List.of()) : List.of(),
                    new SubjectReportExporter(new SubjectReportFormatter(), new AtomicTextFileWriter(), folder),
                    CLOCK,
                    new SubjectExportView(console));
            controller.run();
        }
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }

    private static final class DirectTransactions implements TransactionManager {

        @Override
        public <T> T inTransaction(SqlWork<T> work) {
            return read(work);
        }

        @Override
        public <T> T read(SqlWork<T> work) {
            try {
                return work.execute(null);
            } catch (SQLException e) {
                throw new DataAccessException("Unexpected SQL error", e);
            }
        }
    }
}
