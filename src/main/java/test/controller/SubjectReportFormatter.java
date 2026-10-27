package test.controller;

import test.model.AssignedSubject;
import test.model.TeachingLoad;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SubjectReportFormatter {

    static final int CODE_WIDTH = 10;
    static final int NAME_WIDTH = 35;
    static final int GRADE_WIDTH = 20;

    private static final String SCHOOL_TITLE = "COLEGIO INTERNACIONAL \"LITTLE HORIZONS\"";
    private static final String REPORT_TITLE = "REPORTE DE MATERIAS ASIGNADAS - AÑO ";
    private static final String FULL_NAME_LABEL = "Nombre completo: ";
    private static final String DOCUMENT_LABEL = "Documento: ";
    private static final String GENERATED_LABEL = "Documento generado el: ";
    private static final String ELLIPSIS = "…";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public List<String> format(TeachingLoad load, LocalDate generatedOn) {
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(generatedOn, "generatedOn");
        List<String> lines = new ArrayList<>();
        lines.add(SCHOOL_TITLE);
        lines.add(REPORT_TITLE + load.academicYear());
        lines.add("");
        lines.add(FULL_NAME_LABEL + load.teacher().fullName());
        lines.add(DOCUMENT_LABEL + load.teacher().document());
        lines.add("");
        lines.add(row("Código", "Nombre", "Grado"));
        for (AssignedSubject subject : load.subjects()) {
            lines.add(row(subject.code(), subject.name(), subject.grade()));
        }
        lines.add("");
        lines.add(GENERATED_LABEL + DATE_FORMAT.format(generatedOn));
        return List.copyOf(lines);
    }

    private static String row(String code, String name, String grade) {
        return column(code, CODE_WIDTH) + column(name, NAME_WIDTH) + column(grade, GRADE_WIDTH);
    }

    static String column(String value, int width) {
        String text = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (text.length() >= width) {
            text = text.substring(0, width - 2).stripTrailing() + ELLIPSIS;
        }
        return text + " ".repeat(width - text.length());
    }
}
