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
 * Persistence of {@code pagos} and the read queries used by morosidad.
 */
public interface PaymentDao {

    int insert(Connection connection, Payment payment) throws SQLException;

    void setReceiptNumber(Connection connection, int paymentId, String receiptNumber) throws SQLException;

    Optional<Payment> findById(Connection connection, int paymentId) throws SQLException;

    Optional<Payment> findByReceipt(Connection connection, String receiptNumber) throws SQLException;

    /** Same student, concept, amount, date and method: probably a retry of the same payment. */
    Optional<Payment> findSimilar(Connection connection, int studentId, int conceptId, BigDecimal amount,
                                  LocalDate date, PaymentMethod method) throws SQLException;

    /** Payments of a student; {@code academicYear} null means all years. Newest first. */
    List<Payment> findByStudent(Connection connection, int studentId, Integer academicYear) throws SQLException;

    /** Payments made by the guardian or for any student under the guardian. Newest first. */
    List<Payment> findByGuardian(Connection connection, int guardianId) throws SQLException;

    /** Payments of a year (by academic year, or by payment date when it is missing). */
    List<Payment> findByYear(Connection connection, int year) throws SQLException;

    /** Students with an active enrollment in the year and their main financial guardian. */
    List<BillableStudent> findBillableStudents(Connection connection, int academicYear) throws SQLException;

    /** Student with an active enrollment, used to infer overdue obligations. */
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
