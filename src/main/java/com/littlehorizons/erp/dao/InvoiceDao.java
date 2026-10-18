package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.finance.Invoice;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of {@code facturas}.
 */
public interface InvoiceDao {

    /**
     * Inserts the invoice with a temporary unique number and returns its id;
     * the caller then assigns the final consecutive derived from the id.
     */
    int insertDraft(Connection connection, int paymentId, LocalDate issueDate, LocalDateTime issuedAt)
            throws SQLException;

    void assignNumberAndCufe(Connection connection, int invoiceId, String number, String cufe) throws SQLException;

    void setPdfFile(Connection connection, int invoiceId, String fileName, String path) throws SQLException;

    Optional<Invoice> findById(Connection connection, int invoiceId) throws SQLException;

    Optional<Invoice> findByPayment(Connection connection, int paymentId) throws SQLException;

    /** Invoices whose PDF could not be generated yet (recovery list). */
    List<Invoice> findWithoutPdf(Connection connection) throws SQLException;
}
