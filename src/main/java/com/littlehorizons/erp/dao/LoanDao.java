package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.resources.BorrowerType;
import com.littlehorizons.erp.model.resources.Loan;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de las dos tablas de préstamos del esquema oficial:
 * {@code prestamos_estudiante} y {@code prestamos_profesor}.
 */
public interface LoanDao {

    int insert(Connection connection, Loan loan) throws SQLException;

    Optional<Loan> findById(Connection connection, BorrowerType type, int loanId) throws SQLException;

    /** Préstamos del recurso que aún no se han devuelto, en cualquiera de las dos tablas. */
    List<Loan> findOpenByResource(Connection connection, int resourceId) throws SQLException;

    void markReturned(Connection connection, BorrowerType type, int loanId, LocalDate returnDate, String notes)
            throws SQLException;

    /**
     * Pone el estado "Vencido" a los préstamos activos de ambas tablas cuya fecha límite
     * es anterior a {@code today}.
     *
     * @return cantidad de préstamos actualizados
     */
    int markOverdue(Connection connection, LocalDate today) throws SQLException;

    /** Préstamos no devueltos (Activo o Vencido) de ambas tablas, primero los de fecha límite más antigua. */
    List<Loan> findOpen(Connection connection) throws SQLException;

    /** Historial de préstamos de quien pidió prestado, del más reciente al más antiguo. */
    List<Loan> findByBorrower(Connection connection, BorrowerType type, int borrowerId) throws SQLException;
}
