package test.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record TeachingLoad(TeacherSummary teacher, int academicYear, List<AssignedSubject> subjects) {

    private static final String EXPORT_FILE_NAME = "materias_profesor_%d_%d.txt";

    public TeachingLoad {
        Objects.requireNonNull(teacher, "teacher");
        subjects = List.copyOf(subjects);
    }

    public boolean hasSubjects() {
        return !subjects.isEmpty();
    }

    public String exportFileName() {
        return String.format(Locale.ROOT, EXPORT_FILE_NAME, teacher.id(), academicYear);
    }
}
