package test.controller;

import test.model.TeachingLoad;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

public final class SubjectReportExporter {

    private final SubjectReportFormatter formatter;
    private final AtomicTextFileWriter writer;
    private final Path outputDirectory;

    public SubjectReportExporter(SubjectReportFormatter formatter, AtomicTextFileWriter writer, Path outputDirectory) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
    }

    public Path export(TeachingLoad load, LocalDate generatedOn) throws IOException {
        return writer.write(outputDirectory.resolve(load.exportFileName()), formatter.format(load, generatedOn));
    }
}
