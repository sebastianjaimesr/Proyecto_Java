package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.report.CourseAverageReport;
import com.littlehorizons.erp.model.report.SubjectAverage;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cálculos puros de promedios sobre registros de notas, sin acceso a la base de datos.
 *
 * <p>Reglas (documentadas en el README):</p>
 * <ul>
 * <li>Promedio de materia: la nota Final guardada si existe; si no, la
 *       media de los períodos I a IV que tengan nota.</li>
 * <li>Promedio general de un estudiante: media de los promedios de materia. Las materias
 *       sin notas se excluyen (no cuentan como cero).</li>
 * <li>Promedio del curso en una materia: media de los promedios de materia de los
 *       estudiantes con notas.</li>
 * </ul>
 */
public class AverageCalculator {

    private final AverageStrategy strategy;

    public AverageCalculator(AverageStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    /** Media solo de los períodos regulares; se usa para calcular la nota Final. */
    public BigDecimal periodMean(Collection<GradeRecord> records) {
        return strategy.average(records.stream()
                .filter(gradeRecord -> gradeRecord.period().isEditable())
                .map(GradeRecord::value)
                .collect(Collectors.toList()));
    }

    /** Promedio de una materia a partir de los registros de un estudiante en un año. */
    public SubjectAverage subjectAverage(int subjectId, String subjectName, Collection<GradeRecord> records) {
        Map<GradingPeriod, BigDecimal> grades = records.stream()
                .collect(Collectors.toMap(GradeRecord::period, GradeRecord::value,
                        (first, second) -> second, () -> new EnumMap<>(GradingPeriod.class)));
        BigDecimal finalGrade = grades.get(GradingPeriod.FINAL);
        BigDecimal average = finalGrade != null ? finalGrade : periodMean(records);
        return new SubjectAverage(subjectId, subjectName, grades, average, finalGrade != null);
    }

    /** Un {@link SubjectAverage} por materia, ordenado por nombre de materia. */
    public List<SubjectAverage> subjectAverages(Collection<GradeRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(GradeRecord::subjectId))
                .values().stream()
                .map(subjectRecords -> subjectAverage(subjectRecords.get(0).subjectId(),
                        subjectRecords.get(0).subjectName(), subjectRecords))
                .sorted(Comparator.comparing(SubjectAverage::subjectName))
                .collect(Collectors.toList());
    }

    /** Media de los promedios de materia que existan; {@code null} si ninguna tiene notas. */
    public BigDecimal generalAverage(Collection<SubjectAverage> subjects) {
        return strategy.average(subjects.stream()
                .filter(SubjectAverage::hasGrades)
                .map(SubjectAverage::average)
                .collect(Collectors.toList()));
    }

    /** Promedio de cada estudiante en una materia; los registros deben ser de una sola materia y año. */
    public List<CourseAverageReport.StudentGrade> studentGrades(Collection<GradeRecord> subjectRecords) {
        return subjectRecords.stream()
                .collect(Collectors.groupingBy(GradeRecord::studentId))
                .values().stream()
                .map(studentRecords -> {
                    GradeRecord first = studentRecords.get(0);
                    SubjectAverage average = subjectAverage(first.subjectId(), first.subjectName(), studentRecords);
                    return new CourseAverageReport.StudentGrade(first.studentId(), first.studentName(),
                            average.average(), average.finalGrade());
                })
                .sorted(Comparator.comparing(CourseAverageReport.StudentGrade::studentName))
                .collect(Collectors.toList());
    }

    public BigDecimal courseAverage(Collection<CourseAverageReport.StudentGrade> students) {
        return strategy.average(students.stream()
                .map(CourseAverageReport.StudentGrade::average)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public Optional<BigDecimal> highest(Collection<CourseAverageReport.StudentGrade> students) {
        return extreme(students, Comparator.naturalOrder());
    }

    public Optional<BigDecimal> lowest(Collection<CourseAverageReport.StudentGrade> students) {
        return extreme(students, Comparator.reverseOrder());
    }

    private static Optional<BigDecimal> extreme(Collection<CourseAverageReport.StudentGrade> students,
                                                Comparator<BigDecimal> order) {
        return students.stream()
                .map(CourseAverageReport.StudentGrade::average)
                .filter(Objects::nonNull)
                .max(order);
    }

    /** Agrupa los registros por año, sin importar el orden en que llegaron. */
    public static Map<Integer, List<GradeRecord>> byYear(Collection<GradeRecord> records) {
        return records.stream().collect(Collectors.groupingBy(GradeRecord::academicYear,
                java.util.TreeMap::new, Collectors.mapping(Function.identity(), Collectors.toList())));
    }
}
