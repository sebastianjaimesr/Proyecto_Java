package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcClubDao;
import com.littlehorizons.erp.dao.jdbc.JdbcPaymentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcReportDao;
import com.littlehorizons.erp.model.report.GradeEnrollmentCount;
import com.littlehorizons.erp.model.report.SchoolStatistics;
import com.littlehorizons.erp.support.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aggregated reports over a small fixture inserted with SQL.
 */
class ReportIntegrationTest {

    private static ReportService reports;

    @BeforeAll
    static void createDatabase() {
        TestDatabase database = TestDatabase.recreate();
        TransactionManager transactions = database.transactionManager();
        database.execute(
                "INSERT INTO estudiantes (id_estudiante, tipo_documento, numero_documento, primer_nombre, primer_apellido, "
                        + "fecha_nacimiento, genero, fecha_ingreso, estado) VALUES "
                        + "(1, 'TI', 'R1000001', 'Ana', 'Alfa', '2015-01-01', 'F', '2021-01-20', 'Activo'), "
                        + "(2, 'TI', 'R1000002', 'Beto', 'Beta', '2015-02-01', 'M', '2021-01-20', 'Activo'), "
                        + "(3, 'TI', 'R1000003', 'Caro', 'Gama', '2014-03-01', 'F', '2020-01-20', 'Retirado')",
                "INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula) VALUES "
                        + "(1, 6, 2026, 'Activa', '2026-01-20'), (2, 6, 2026, 'Activa', '2026-01-20'), "
                        + "(3, 7, 2026, 'Retirada', '2026-01-20')",
                "INSERT INTO profesores (id_profesor, tipo_documento, numero_documento, primer_nombre, primer_apellido, "
                        + "correo_electronico, fecha_contratacion) VALUES (1, 'CC', 'P1000001', 'Luis', 'Delta', "
                        + "'luis@littlehorizons.edu.co', '2018-01-01')",
                "INSERT INTO materias (id_materia, codigo_materia, nombre_materia, id_grado) VALUES "
                        + "(1, 'MAT-R5', 'Matemáticas', 6), (2, 'ESP-R5', 'Español', 6)",
                "INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, "
                        + "periodo_academico, valor_nota, fecha_registro) VALUES "
                        + "(1, 1, 1, 2026, 'I', 4.0, '2026-04-01'), (1, 2, 1, 2026, 'I', 5.0, '2026-04-01'), "
                        + "(2, 1, 1, 2026, 'I', 3.0, '2026-04-01'), (2, 1, 1, 2026, 'II', 3.4, '2026-06-01')",
                "INSERT INTO acudientes (id_acudiente, numero_documento, primer_nombre, primer_apellido, parentesco, "
                        + "telefono_principal) VALUES (1, 'A1000001', 'Marta', 'Alfa', 'Madre', '3000000001')",
                "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, "
                        + "numero_recibo, año_academico) VALUES "
                        + "(1, 1, 1, 500000, '2026-01-20', 'Efectivo', 'R-1', 2026), "
                        + "(1, 1, 2, 350000, '2026-02-05', 'Transferencia', 'R-2', 2026), "
                        + "(2, NULL, 2, 350000, '2026-02-07', 'Transferencia', 'R-3', 2026)",
                "INSERT INTO clubes (nombre_club, capacidad_maxima) VALUES ('Ajedrez', 4)",
                "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso) VALUES (1, 1, '2026-02-01')");
        reports = new ReportService(transactions, new JdbcReportDao(), new JdbcPaymentDao(), new JdbcClubDao(),
                new AverageCalculator(new ArithmeticMeanStrategy()));
    }

    @Test
    void enrollmentsAreCountedPerGradeIncludingEmptyGrades() {
        List<GradeEnrollmentCount> rows = reports.enrollmentsByGrade(2026);

        assertEquals(12, rows.size(), "Preescolar to Once are always listed");
        GradeEnrollmentCount fifth = rows.stream().filter(row -> row.level() == 5).findFirst().orElseThrow();
        assertEquals(2, fifth.active());
        GradeEnrollmentCount sixth = rows.stream().filter(row -> row.level() == 6).findFirst().orElseThrow();
        assertEquals(1, sixth.withdrawn());
    }

    @Test
    void rankingUsesTheGeneralAverageRules() {
        List<ReportService.StudentRanking> ranking = reports.ranking(2026, 10);

        assertEquals(2, ranking.size());
        assertEquals(1, ranking.get(0).studentId());
        assertEquals(new BigDecimal("4.5"), ranking.get(0).average());
        // Student 2: Matemáticas (3.0 + 3.4) / 2 = 3.2
        assertEquals(new BigDecimal("3.2"), ranking.get(1).average());
        assertEquals(2, ranking.get(1).position());
    }

    @Test
    void paymentsAreGroupedByConceptMethodAndMonth() {
        ReportService.PaymentSummary summary = reports.paymentSummary(2026);

        assertEquals(new BigDecimal("1200000.00"), summary.total());
        assertEquals(3, summary.count());
        assertEquals(new BigDecimal("700000.00"), summary.byConcept().get("Pensión Mensual"));
        assertEquals(new BigDecimal("700000.00"), summary.byMethod().get("Transferencia"));
        assertEquals(new BigDecimal("700000.00"), summary.byMonth().get(2));
    }

    @Test
    void generalStatistics() {
        SchoolStatistics stats = reports.statistics(2026);

        assertEquals(2, stats.activeStudents());
        assertEquals(2, stats.activeEnrollments());
        assertEquals(1, stats.activeTeachers());
        assertEquals(1, stats.activeClubMembers());
        assertEquals(4, stats.gradeRecords());
        assertEquals(new BigDecimal("3.9"), stats.averageGrade());
        assertTrue(stats.paymentsTotal().compareTo(new BigDecimal("1200000")) == 0);
        assertEquals(new BigDecimal("25.0"), reports.clubOccupancy().get(0).occupancyPercent());
    }
}
