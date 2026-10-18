package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of the two loan tables of the official schema:
 * {@code prestamos_estudiante} and {@code prestamos_profesor}.
 */
public interface LoanDao {

    int insert(Connection connection, Loan loan) throws SQLException;

    Optional<Loan> findById(Connection connection, BorrowerType type, int loanId) throws SQLException;

    /** Loans of the resource not yet returned, in either table. */
    List<Loan> findOpenByResource(Connection connection, int resourceId) throws SQLException;

    void markReturned(Connection connection, BorrowerType type, int loanId, LocalDate returnDate, String notes)
            throws SQLException;

    /**
     * Sets status "Vencido" on active loans of both tables whose due date is
     * before {@code today}.
     *
     * @return number of updated loans
     */
    int markOverdue(Connection connection, LocalDate today) throws SQLException;

    /** Loans not returned (Activo or Vencido) of both tables, oldest due date first. */
    List<Loan> findOpen(Connection connection) throws SQLException;

    /** Loan history of a borrower, newest first. */
    List<Loan> findByBorrower(Connection connection, BorrowerType type, int borrowerId) throws SQLException;
}
