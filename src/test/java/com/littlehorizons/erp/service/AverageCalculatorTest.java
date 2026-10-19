package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.academic.GradingPeriod;
import com.littlehorizons.erp.model.report.CourseAverageReport;
import com.littlehorizons.erp.model.report.SubjectAverage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AverageCalculatorTest {

    private final AverageCalculator calculator = new AverageCalculator(new ArithmeticMeanStrategy());

    private static GradeRecord grade(int studentId, int subjectId, GradingPeriod period, String value) {
        return new GradeRecord(null, studentId, subjectId, 1, 2026, period, new BigDecimal(value), null,
                LocalDate.of(2026, 6, 1), "Estudiante " + studentId, "Materia " + subjectId, "Profesor");
    }

    @Test
    void meanIsRoundedHalfUpToOneDecimal() {
        ArithmeticMeanStrategy strategy = new ArithmeticMeanStrategy();
        // (3.4 + 3.5) / 2 = 3.45 -> 3.5
        assertEquals(new BigDecimal("3.5"), strategy.average(List.of(new BigDecimal("3.4"), new BigDecimal("3.5"))));
        // (4.0 + 4.1 + 4.1) / 3 = 4.0666 -> 4.1
        assertEquals(new BigDecimal("4.1"), strategy.average(List.of(new BigDecimal("4.0"), new BigDecimal("4.1"),
                new BigDecimal("4.1"))));
        assertNull(strategy.average(List.of()));
    }

    @Test
    void subjectAverageUsesPeriodsWhenThereIsNoFinal() {
        SubjectAverage average = calculator.subjectAverage(10, "Matemáticas", List.of(
                grade(1, 10, GradingPeriod.I, "3.0"),
                grade(1, 10, GradingPeriod.II, "4.0")));

        assertEquals(new BigDecimal("3.5"), average.average());
        assertFalse(average.finalGrade());
        assertEquals(2, average.gradedPeriods());
    }

    @Test
    void storedFinalGradeTakesPrecedence() {
        SubjectAverage average = calculator.subjectAverage(10, "Matemáticas", List.of(
                grade(1, 10, GradingPeriod.I, "3.0"),
                grade(1, 10, GradingPeriod.II, "4.0"),
                grade(1, 10, GradingPeriod.FINAL, "3.8")));

        assertEquals(new BigDecimal("3.8"), average.average());
        assertTrue(average.finalGrade());
        assertEquals(new BigDecimal("3.5"), calculator.periodMean(List.of(
                grade(1, 10, GradingPeriod.I, "3.0"), grade(1, 10, GradingPeriod.II, "4.0"),
                grade(1, 10, GradingPeriod.FINAL, "1.0"))), "Final is ignored when computing the period mean");
    }

    @Test
    void generalAverageIgnoresSubjectsWithoutGrades() {
        List<SubjectAverage> subjects = List.of(
                new SubjectAverage(1, "A", Map.of(GradingPeriod.I, new BigDecimal("4.0")), new BigDecimal("4.0"), false),
                new SubjectAverage(2, "B", Map.of(GradingPeriod.I, new BigDecimal("3.0")), new BigDecimal("3.0"), false),
                new SubjectAverage(3, "C", Map.of(), null, false));

        assertEquals(new BigDecimal("3.5"), calculator.generalAverage(subjects));
    }

    @Test
    void subjectAveragesAreGroupedAndSorted() {
        List<SubjectAverage> averages = calculator.subjectAverages(List.of(
                grade(1, 20, GradingPeriod.I, "5.0"),
                grade(1, 10, GradingPeriod.I, "2.0"),
                grade(1, 10, GradingPeriod.II, "3.0")));

        assertEquals(2, averages.size());
        assertEquals("Materia 10", averages.get(0).subjectName());
        assertEquals(new BigDecimal("2.5"), averages.get(0).average());
        assertEquals(new BigDecimal("5.0"), averages.get(1).average());
    }

    @Test
    void courseAverageAndExtremes() {
        List<CourseAverageReport.StudentGrade> students = calculator.studentGrades(List.of(
                grade(1, 10, GradingPeriod.I, "2.0"),
                grade(1, 10, GradingPeriod.II, "3.0"),
                grade(2, 10, GradingPeriod.I, "4.6"),
                grade(3, 10, GradingPeriod.FINAL, "3.9")));

        assertEquals(3, students.size());
        // estudiantes: 2.5, 4.6, 3.9 -> 3.666 -> 3.7
        assertEquals(new BigDecimal("3.7"), calculator.courseAverage(students));
        assertEquals(new BigDecimal("4.6"), calculator.highest(students).orElseThrow());
        assertEquals(new BigDecimal("2.5"), calculator.lowest(students).orElseThrow());
    }

    @Test
    void isoWeekGoesFromMondayToSunday() {
        AttendanceService.Week week = AttendanceService.Week.containing(LocalDate.of(2026, 10, 15));
        assertEquals(LocalDate.of(2026, 10, 12), week.monday());
        assertEquals(LocalDate.of(2026, 10, 18), week.sunday());
        assertEquals(week, AttendanceService.Week.containing(LocalDate.of(2026, 10, 18)));
        assertEquals(LocalDate.of(2026, 10, 19), AttendanceService.Week.containing(LocalDate.of(2026, 10, 19)).monday());
    }
}
