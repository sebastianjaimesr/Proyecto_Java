package test.dao;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.support.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.model.AssignedSubject;
import test.model.TeacherSummary;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSubjectExportIntegrationTest {

    private static TransactionManager transactions;

    private final TeacherSummaryDao teacherDao = new JdbcTeacherSummaryDao();
    private final AssignedSubjectDao subjectDao = new JdbcAssignedSubjectDao();

    @BeforeAll
    static void prepareDatabase() {
        TestDatabase database = TestDatabase.recreate();
        database.execute(
                "INSERT INTO profesores (id_profesor, tipo_documento, numero_documento, primer_nombre, "
                        + "segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, fecha_contratacion) "
                        + "VALUES (1, 'CC', '80123456', 'Roberto', 'Andrés', 'Martínez', 'Torres', "
                        + "'roberto.martinez@prueba.co', '2020-01-15'), "
                        + "(2, 'Pasaporte', 'AB123', 'Lina', NULL, 'Gómez', NULL, 'lina.gomez@prueba.co', '2021-02-01')",
                "INSERT INTO materias (id_materia, codigo_materia, nombre_materia, id_grado) VALUES "
                        + "(1, 'MAT-10', 'Matemáticas', (SELECT id_grado FROM grados WHERE nivel_numerico = 10)), "
                        + "(2, 'MAT-11', 'Matemáticas', (SELECT id_grado FROM grados WHERE nivel_numerico = 11)), "
                        + "(3, 'FIS-10', 'Física', (SELECT id_grado FROM grados WHERE nivel_numerico = 10))",
                "INSERT INTO asignaciones_profesor (id_asignacion, id_profesor, id_materia, año_academico, seccion) "
                        + "VALUES (1, 1, 1, 2025, 'A'), (2, 1, 2, 2025, 'A'), (3, 1, 3, 2025, 'A'), "
                        + "(4, 1, 1, 2025, 'B'), (5, 1, 2, 2026, 'A'), (6, 2, 3, 2026, 'A')");
        transactions = database.transactionManager();
    }

    @Test
    void findsTeacherWithDocumentAndFullName() {
        TeacherSummary roberto = transactions.read(connection -> teacherDao.findById(connection, 1)).orElseThrow();
        TeacherSummary lina = transactions.read(connection -> teacherDao.findById(connection, 2)).orElseThrow();

        assertEquals("Roberto Andrés Martínez Torres", roberto.fullName());
        assertEquals("CC 80123456", roberto.document());
        assertEquals("Lina Gómez", lina.fullName());
        assertEquals("Pasaporte AB123", lina.document());
    }

    @Test
    void unknownTeacherIsEmpty() {
        Optional<TeacherSummary> missing = transactions.read(connection -> teacherDao.findById(connection, 999));

        assertTrue(missing.isEmpty());
    }

    @Test
    void subjectsAreFilteredByYearWithoutDuplicatesInAssignmentOrder() {
        List<AssignedSubject> subjects = transactions.read(
                connection -> subjectDao.findByTeacherAndYear(connection, 1, 2025));

        assertEquals(List.of(
                new AssignedSubject("MAT-10", "Matemáticas", "Décimo"),
                new AssignedSubject("MAT-11", "Matemáticas", "Once"),
                new AssignedSubject("FIS-10", "Física", "Décimo")), subjects);
    }

    @Test
    void teacherWithAssignmentsOnlyInOtherYearsHasNoSubjectsIn2025() {
        assertTrue(transactions.read(connection -> subjectDao.findByTeacherAndYear(connection, 2, 2025)).isEmpty());
    }
}
