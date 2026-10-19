package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.model.academic.GradeRecord;
import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.report.EventTypeParticipation;
import com.littlehorizons.erp.model.report.GradeEnrollmentCount;
import com.littlehorizons.erp.model.report.SchoolStatistics;
import com.littlehorizons.erp.model.report.SubjectAverage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Reportes agregados. Los conteos sobre tablas completas se delegan a SQL
 * ({@link ReportDao}); los Streams se usan donde Java aporta: el ranking con las
 * mismas reglas de promedio del módulo académico y la agrupación de pagos.
 */
public class ReportService {

    private final TransactionManager transactions;
    private final ReportDao reportDao;
    private final PaymentDao paymentDao;
    private final ClubDao clubDao;
    private final AverageCalculator calculator;

    public ReportService(TransactionManager transactions, ReportDao reportDao, PaymentDao paymentDao, ClubDao clubDao,
                         AverageCalculator calculator) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.reportDao = Objects.requireNonNull(reportDao, "reportDao");
        this.paymentDao = Objects.requireNonNull(paymentDao, "paymentDao");
        this.clubDao = Objects.requireNonNull(clubDao, "clubDao");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
    }

    /** Promedio general de un estudiante, usado en el ranking. */
    public record StudentRanking(int position, int studentId, String studentName, BigDecimal average, int subjects) {
    }

    /** Pagos de un año agrupados de tres formas. */
    public record PaymentSummary(
            int academicYear,
            BigDecimal total,
            long count,
            Map<String, BigDecimal> byConcept,
            Map<String, BigDecimal> byMethod,
            Map<Integer, BigDecimal> byMonth) {
    }

    /** Ocupación de los clubes. */
    public record ClubOccupancy(String clubName, int activeMembers, int capacity, BigDecimal occupancyPercent,
                                boolean active) {
    }

    /** Matrículas por grado en un año, separadas por estado. */
    public List<GradeEnrollmentCount> enrollmentsByGrade(int academicYear) {
        return transactions.read(connection -> reportDao.enrollmentsByGrade(connection, academicYear));
    }

    /** Cifras generales del colegio en un año. */
    public SchoolStatistics statistics(int academicYear) {
        return transactions.read(connection -> reportDao.statistics(connection, academicYear));
    }

    /** Eventos y participaciones por tipo de evento en un año. */
    public List<EventTypeParticipation> eventParticipationByType(int year) {
        return transactions.read(connection -> reportDao.eventParticipationByType(connection, year));
    }

    /**
     * Estudiantes ordenados por promedio general del año (mismas reglas del
     * reporte por estudiante: nota Final si existe, materias sin notas
     * excluidas). Los empates conservan el orden alfabético.
     */
    public List<StudentRanking> ranking(int academicYear, int limit) {
        List<GradeRecord> records = transactions.read(connection -> reportDao.gradesOfYear(connection, academicYear));
        List<StudentRanking> unnumbered = records.stream()
                .collect(Collectors.groupingBy(GradeRecord::studentId))
                .values().stream()
                .map(studentRecords -> {
                    List<SubjectAverage> subjects = calculator.subjectAverages(studentRecords);
                    return new StudentRanking(0, studentRecords.get(0).studentId(), studentRecords.get(0).studentName(),
                            calculator.generalAverage(subjects), subjects.size());
                })
                .filter(ranking -> ranking.average() != null)
                .sorted(Comparator.comparing(StudentRanking::average).reversed()
                        .thenComparing(StudentRanking::studentName))
                .limit(limit)
                .collect(Collectors.toList());
        return java.util.stream.IntStream.range(0, unnumbered.size())
                .mapToObj(i -> {
                    StudentRanking item = unnumbered.get(i);
                    return new StudentRanking(i + 1, item.studentId(), item.studentName(), item.average(), item.subjects());
                })
                .collect(Collectors.toList());
    }

    /**
     * Total recaudado en un año y el mismo total agrupado por concepto, método de pago y mes (con Streams).
     */
    public PaymentSummary paymentSummary(int academicYear) {
        List<Payment> payments = transactions.read(connection -> paymentDao.findByYear(connection, academicYear));
        BigDecimal total = payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> byConcept = payments.stream().collect(Collectors.groupingBy(Payment::conceptName,
                TreeMap::new, Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)));
        Map<String, BigDecimal> byMethod = payments.stream().collect(Collectors.groupingBy(
                payment -> payment.method().dbValue(), TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)));
        Map<Integer, BigDecimal> byMonth = payments.stream().collect(Collectors.groupingBy(
                payment -> payment.paymentDate().getMonthValue(), TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)));
        return new PaymentSummary(academicYear, total, payments.size(), byConcept, byMethod, byMonth);
    }

    /** Ocupación de cada club: miembros activos, cupo y porcentaje de ocupación. */
    public List<ClubOccupancy> clubOccupancy() {
        List<Club> clubs = transactions.read(connection -> clubDao.findAll(connection, false));
        return clubs.stream()
                .map(club -> new ClubOccupancy(club.name(), club.activeMembers(), club.capacity(),
                        BigDecimal.valueOf(club.activeMembers() * 100L)
                                .divide(BigDecimal.valueOf(Math.max(1, club.capacity())), 1, java.math.RoundingMode.HALF_UP),
                        club.active()))
                .sorted(Comparator.comparing(ClubOccupancy::occupancyPercent).reversed()
                        .thenComparing(ClubOccupancy::clubName))
                .collect(Collectors.toList());
    }
}
