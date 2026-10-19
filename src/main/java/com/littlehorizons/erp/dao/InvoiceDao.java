package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.finance.Invoice;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de {@code facturas}.
 */
public interface InvoiceDao {

    /**
     * Inserta la factura con un número único temporal y devuelve su id;
     * luego quien la llama asigna el consecutivo final, derivado del id.
     */
    int insertDraft(Connection connection, int paymentId, LocalDate issueDate, LocalDateTime issuedAt)
            throws SQLException;

    void assignNumberAndCufe(Connection connection, int invoiceId, String number, String cufe) throws SQLException;

    void setPdfFile(Connection connection, int invoiceId, String fileName, String path) throws SQLException;

    Optional<Invoice> findById(Connection connection, int invoiceId) throws SQLException;

    Optional<Invoice> findByPayment(Connection connection, int paymentId) throws SQLException;

    /** Facturas cuyo PDF todavía no se pudo generar (lista de recuperación). */
    List<Invoice> findWithoutPdf(Connection connection) throws SQLException;
}
