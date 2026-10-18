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
 * Pure average calculations over grade records, with no database access.
 *
 * <p>Rules (documented in the README):</p>
 * <ul>
 *   <li>Subject average: the stored Final grade when it exists; otherwise the
 *       mean of the periods I to IV that have a grade.</li>
 *   <li>General average of a student: mean of the subject averages. Subjects
 *       without grades are excluded (not counted as zero).</li>
 *   <li>Course average of a subject: mean of the subject averages of the
 *       students with grades.</li>
 * </ul>
 */
public class AverageCalculator {

    private final AverageStrategy strategy;

    public AverageCalculator(AverageStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    /** Mean of the regular periods only; used to compute the Final grade. */
    public BigDecimal periodMean(Collection<GradeRecord> records) {
        return strategy.average(records.stream()
                .filter(gradeRecord -> gradeRecord.period().isEditable())
                .map(GradeRecord::value)
                .collect(Collectors.toList()));
    }

    /** Average of one subject from the records of one student and year. */
    public SubjectAverage subjectAverage(int subjectId, String subjectName, Collection<GradeRecord> records) {
        Map<GradingPeriod, BigDecimal> grades = records.stream()
                .collect(Collectors.toMap(GradeRecord::period, GradeRecord::value,
                        (first, second) -> second, () -> new EnumMap<>(GradingPeriod.class)));
        BigDecimal finalGrade = grades.get(GradingPeriod.FINAL);
        BigDecimal average = finalGrade != null ? finalGrade : periodMean(records);
        return new SubjectAverage(subjectId, subjectName, grades, average, finalGrade != null);
    }

    /** One {@link SubjectAverage} per subject, ordered by subject name. */
    public List<SubjectAverage> subjectAverages(Collection<GradeRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(GradeRecord::subjectId))
                .values().stream()
                .map(subjectRecords -> subjectAverage(subjectRecords.get(0).subjectId(),
                        subjectRecords.get(0).subjectName(), subjectRecords))
                .sorted(Comparator.comparing(SubjectAverage::subjectName))
                .collect(Collectors.toList());
    }

    /** Mean of the subject averages that exist; {@code null} when none has grades. */
    public BigDecimal generalAverage(Collection<SubjectAverage> subjects) {
        return strategy.average(subjects.stream()
                .filter(SubjectAverage::hasGrades)
                .map(SubjectAverage::average)
                .collect(Collectors.toList()));
    }

    /** Average of every student in one subject; records must belong to one subject and year. */
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

    /** Groups records by year, keeping insertion order irrelevant. */
    public static Map<Integer, List<GradeRecord>> byYear(Collection<GradeRecord> records) {
        return records.stream().collect(Collectors.groupingBy(GradeRecord::academicYear,
                java.util.TreeMap::new, Collectors.mapping(Function.identity(), Collectors.toList())));
    }
}
