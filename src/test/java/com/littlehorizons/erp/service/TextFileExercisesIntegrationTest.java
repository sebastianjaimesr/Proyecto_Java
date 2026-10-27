package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.common.ImportResult;
import com.littlehorizons.erp.support.TestDatabase;
import com.littlehorizons.erp.util.TextFileWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text file exercises of the student module against the MySQL test database.
 */
class TextFileExercisesIntegrationTest {

    @TempDir
    static Path folder;

    private static StudentService students;

    @BeforeAll
    static void createDatabase() {
        TestDatabase database = TestDatabase.recreate();
        students = new StudentService(database.transactionManager(), new JdbcStudentDao(), new JdbcGuardianDao(),
                new JdbcEnrollmentDao(), TestDatabase.CLOCK);
    }

    @Test
    @DisplayName("Exercise 15: good lines are saved, bad lines are reported with their number")
    void importStudentsReportsEachBadLine() throws IOException {
        Path file = folder.resolve("estudiantes.txt");
        TextFileWriter.write(file, List.of(
                "# tipoDoc;documento;primerNombre;segundoNombre;primerApellido;segundoApellido;nacimiento;genero;dir;tel",
                "TI;1099100001;Laura;Sofía;Gómez;Ruiz;2015-04-12;F;Calle 10 # 5-20;3001234567",
                "ti;1099100002;Martín;;Pérez;;2014-11-30;m;;",
                "TI;1099100001;Repetido;;Doc;;2015-01-01;F;;",
                "TI;1099100003;Ana;;Díaz;;2015-02-30;F;;",
                "TI;1099100004;Juan;;Mora;;2015-01-01;X;;",
                "TI;1099100005;Faltan;Campos"));

        ImportResult result = students.importFromFile(file);

        assertEquals(6, result.processed());
        assertEquals(2, result.saved());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Línea 4: Ya existe un estudiante"));
        assertTrue(result.errors().get(1).startsWith("Línea 5:"));
        assertTrue(result.errors().get(2).contains("género no válido"));
        assertTrue(result.errors().get(3).contains("se esperaban 10 campos"));
        Student martin = students.findByDocument("1099100002");
        assertEquals(Gender.MALE, martin.gender());
    }

    @Test
    @DisplayName("Exercise 15: a missing file is a clear validation message")
    void importFromMissingFile() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> students.importFromFile(folder.resolve("no-existe.txt")));
        assertTrue(error.getMessage().startsWith("El archivo no existe"));
    }

    @Test
    @DisplayName("Exercises 3 and 4: no certificate without enrollment; medical data only when asked")
    void certificateAndProfile() throws IOException {
        Path file = folder.resolve("uno.txt");
        TextFileWriter.write(file, List.of("TI;1099100010;Sara;;Medina;;2016-05-05;F;;"));
        students.importFromFile(file);
        Student sara = students.findByDocument("1099100010");

        assertThrows(BusinessRuleException.class, () -> students.exportCertificate(sara.id()));

        Path without = students.exportProfile(sara.id(), false);
        assertFalse(Files.readString(without).contains("DATOS MÉDICOS"));
        Path with = students.exportProfile(sara.id(), true);
        assertTrue(Files.readString(with).contains("DATOS MÉDICOS"));
        assertTrue(with.toString().contains("test-exports"), "tests must not write to the real exports folder");
    }
}
