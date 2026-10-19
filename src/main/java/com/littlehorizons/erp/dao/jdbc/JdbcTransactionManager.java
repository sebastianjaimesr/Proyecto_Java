package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.SqlWork;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;

/**
 * Implementación JDBC de {@link TransactionManager}.
 *
 * <p>Cada llamada abre una conexión nueva con try-with-resources. Las transacciones
 * usan aislamiento {@code READ COMMITTED}; las secciones críticas (cupo de clubes,
 * préstamos de recursos) además bloquean la fila padre con
 * {@code SELECT ... FOR UPDATE}.</p>
 */
public class JdbcTransactionManager implements TransactionManager {

    /** Códigos de error de MySQL que se traducen a mensajes de negocio. */
    private static final int ER_DUP_ENTRY = 1062;
    private static final int ER_ROW_IS_REFERENCED = 1451;
    private static final int ER_NO_REFERENCED_ROW = 1452;
    private static final int ER_CHECK_CONSTRAINT_VIOLATED = 3819;

    private final ConnectionProvider connectionProvider;

    public JdbcTransactionManager(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    @Override
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public <T> T read(SqlWork<T> work) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setReadOnly(true);
            return work.execute(connection);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private static void rollbackQuietly(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
            AppLogger.logError(JdbcTransactionManager.class, "Rollback failed", rollbackError);
        }
    }

    /**
     * Las violaciones de restricciones se convierten en mensajes de negocio (la base de datos es la
     * última barrera contra duplicados); todo lo demás es un error técnico.
     */
    static RuntimeException translate(SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException || e.getErrorCode() == ER_CHECK_CONSTRAINT_VIOLATED) {
            switch (e.getErrorCode()) {
                case ER_DUP_ENTRY:
                    return new BusinessRuleException(
                            "Ya existe un registro con esos datos (documento, código o combinación única).");
                case ER_ROW_IS_REFERENCED:
                    return new BusinessRuleException(
                            "No se puede completar la operación porque el registro tiene información relacionada.");
                case ER_NO_REFERENCED_ROW:
                    return new BusinessRuleException("Uno de los registros relacionados no existe.");
                case ER_CHECK_CONSTRAINT_VIOLATED:
                    return new BusinessRuleException("Un valor está fuera del rango permitido por la base de datos.");
                default:
                    break;
            }
        }
        return new DataAccessException("Database operation failed (SQLState " + e.getSQLState()
                + ", code " + e.getErrorCode() + ")", e);
    }
}
