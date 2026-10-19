package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.finance.Payment;
import com.littlehorizons.erp.model.finance.PaymentMethod;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code pagos} y consultas de lectura que usa la morosidad.
 */
public interface PaymentDao {

    int insert(Connection connection, Payment payment) throws SQLException;

    void setReceiptNumber(Connection connection, int paymentId, String receiptNumber) throws SQLException;

    Optional<Payment> findById(Connection connection, int paymentId) throws SQLException;

    Optional<Payment> findByReceipt(Connection connection, String receiptNumber) throws SQLException;

    /** Mismo estudiante, concepto, monto, fecha y método: probablemente es un reintento del mismo pago. */
    Optional<Payment> findSimilar(Connection connection, int studentId, int conceptId, BigDecimal amount,
                                  LocalDate date, PaymentMethod method) throws SQLException;

    /** Pagos de un estudiante; {@code academicYear} null significa todos los años. Del más reciente al más antiguo. */
    List<Payment> findByStudent(Connection connection, int studentId, Integer academicYear) throws SQLException;

    /** Pagos hechos por el acudiente o para cualquier estudiante a su cargo. Del más reciente al más antiguo. */
    List<Payment> findByGuardian(Connection connection, int guardianId) throws SQLException;

    /** Pagos de un año (por año académico, o por fecha de pago cuando falta el año). */
    List<Payment> findByYear(Connection connection, int year) throws SQLException;

    /** Estudiantes con matrícula activa en el año y su acudiente financiero principal. */
    List<BillableStudent> findBillableStudents(Connection connection, int academicYear) throws SQLException;

    /** Estudiante con matrícula activa, usado para inferir obligaciones vencidas. */
    record BillableStudent(
            int studentId,
            String document,
            String name,
            String gradeName,
            LocalDate enrollmentDate,
            String guardianName,
            String guardianPhone) {
    }
}
