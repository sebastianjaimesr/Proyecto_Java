package test.model;

import com.littlehorizons.erp.model.academic.PersonDocumentType;
import com.littlehorizons.erp.model.common.PersonName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherSummaryTest {

    @Test
    void fullNameUsesTheFourPartsInOrder() {
        assertEquals("Roberto Andrés Martínez Torres",
                teacher(new PersonName("Roberto", "Andrés", "Martínez", "Torres")).fullName());
    }

    @Test
    void fullNameSkipsMissingPartsWithoutDoubleSpaces() {
        assertEquals("Samuel Sánchez Jiménez", teacher(new PersonName("Samuel", null, "Sánchez", "Jiménez")).fullName());
        assertEquals("Lina Gómez", teacher(new PersonName("Lina", "  ", "Gómez", "")).fullName());
        assertEquals("María José Rojas", teacher(new PersonName(" María  José ", null, " Rojas ", null)).fullName());
    }

    @Test
    void documentShowsTypeSpaceAndNumber() {
        assertEquals("CC 80123456", teacher(new PersonName("Ana", null, "Ruiz", null)).document());
        TeacherSummary passport = new TeacherSummary(2, PersonDocumentType.PASSPORT, " AB123 ",
                new PersonName("Ana", null, "Ruiz", null));
        assertEquals("Pasaporte AB123", passport.document());
    }

    @Test
    void teachingLoadBuildsTheExportFileNameAndProtectsItsList() {
        TeachingLoad load = new TeachingLoad(teacher(new PersonName("Ana", null, "Ruiz", null)), 2025,
                List.of(new AssignedSubject("MAT-10", "Matemáticas", "Décimo")));

        assertEquals("materias_profesor_1_2025.txt", load.exportFileName());
        assertTrue(load.hasSubjects());
        assertFalse(new TeachingLoad(load.teacher(), 2025, List.of()).hasSubjects());
        assertThrows(UnsupportedOperationException.class, () -> load.subjects().clear());
    }

    private static TeacherSummary teacher(PersonName name) {
        return new TeacherSummary(1, PersonDocumentType.CC, "80123456", name);
    }
}
