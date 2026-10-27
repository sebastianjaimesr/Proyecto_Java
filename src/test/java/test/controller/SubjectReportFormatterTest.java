package test.controller;

import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.common.PersonName;
import org.junit.jupiter.api.Test;
import test.model.AssignedSubject;
import test.model.TeacherSummary;
import test.model.TeachingLoad;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubjectReportFormatterTest {

    private final SubjectReportFormatter formatter = new SubjectReportFormatter();

    @Test
    void reportFollowsTheRequiredLayout() {
        TeachingLoad load = new TeachingLoad(
                new TeacherSummary(1, PersonDocumentType.CC, "80123456",
                        new PersonName("Roberto", "Andrés", "Martínez", "Torres")),
                2025,
                List.of(new AssignedSubject("MAT-10", "Matemáticas", "Décimo"),
                        new AssignedSubject("MAT-11", "Matemáticas", "Once"),
                        new AssignedSubject("FIS-10", "Física", "Décimo")));

        List<String> lines = formatter.format(load, LocalDate.of(2026, 10, 7));

        assertEquals(List.of(
                "COLEGIO INTERNACIONAL \"LITTLE HORIZONS\"",
                "REPORTE DE MATERIAS ASIGNADAS - AÑO 2025",
                "",
                "Nombre completo: Roberto Andrés Martínez Torres",
                "Documento: CC 80123456",
                "",
                "Código    Nombre                             Grado               ",
                "MAT-10    Matemáticas                        Décimo              ",
                "MAT-11    Matemáticas                        Once                ",
                "FIS-10    Física                             Décimo              ",
                "",
                "Documento generado el: 07/10/2026"), lines);
    }

    @Test
    void everyTableRowHasTheExactColumnWidths() {
        int rowWidth = SubjectReportFormatter.CODE_WIDTH + SubjectReportFormatter.NAME_WIDTH
                + SubjectReportFormatter.GRADE_WIDTH;
        assertEquals(65, rowWidth);
        assertEquals(10, SubjectReportFormatter.column("MAT-10", 10).length());
        assertEquals(35, SubjectReportFormatter.column("Matemáticas", 35).length());
        assertEquals(20, SubjectReportFormatter.column("Décimo", 20).length());
    }

    @Test
    void longValuesAreShortenedSoColumnsStayAligned() {
        String column = SubjectReportFormatter.column("Ciencias Naturales y Educación Ambiental Avanzada", 35);

        assertEquals(35, column.length());
        assertEquals("Ciencias Naturales y Educación Am… ", column);
        assertEquals("CODIGO-L… ", SubjectReportFormatter.column("CODIGO-LARGO-20", 10));
    }

    @Test
    void decomposedAccentsAreNormalizedBeforeMeasuring() {
        String decomposed = "Decimo".replace("e", "e\u0301");

        assertEquals("Décimo" + " ".repeat(14), SubjectReportFormatter.column(decomposed, 20));
    }
}
