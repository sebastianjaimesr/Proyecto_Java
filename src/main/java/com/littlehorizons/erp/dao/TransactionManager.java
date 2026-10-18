package com.littlehorizons.erp.dao;

/**
 * Runs database work on its own connection.
 *
 * <p>Services use it to group several DAO calls: everything inside
 * {@link #inTransaction(SqlWork)} is committed together or rolled back
 * together. DAO methods receive the connection as a parameter, so the
 * transaction boundary is always visible in the service code.</p>
 */
public interface TransactionManager {

    /**
     * Runs {@code work} in a transaction: commit when it returns, rollback when
     * it throws.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException for SQL errors
     * @throws com.littlehorizons.erp.exception.BusinessRuleException when a
     *         unique or foreign key constraint rejects the data
     */
    <T> T inTransaction(SqlWork<T> work);

    /** Runs read-only work with auto-commit on a fresh connection. */
    <T> T read(SqlWork<T> work);

    /** Convenience for transactional work without a result. */
    default void inTransactionVoid(SqlVoidWork work) {
        inTransaction(connection -> {
            work.execute(connection);
            return null;
        });
    }

    /** Work without a result. */
    @FunctionalInterface
    interface SqlVoidWork {
        void execute(java.sql.Connection connection) throws java.sql.SQLException;
    }
}
