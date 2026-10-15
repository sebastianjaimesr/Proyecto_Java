package com.littlehorizons.erp.util;

import java.sql.Connection;

/**
 * Source of JDBC connections.
 *
 * <p>DAOs depend on this abstraction instead of on {@link DatabaseConnection},
 * so tests can supply another implementation (Dependency Inversion).</p>
 */
@FunctionalInterface
public interface ConnectionProvider {

    /**
     * Opens a new connection. Each call returns a different connection that the
     * caller owns and must close, ideally with try-with-resources; connections
     * are never shared between operations.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException if the
     *         database cannot be reached
     */
    Connection getConnection();
}
