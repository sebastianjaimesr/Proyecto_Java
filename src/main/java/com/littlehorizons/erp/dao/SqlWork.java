package com.littlehorizons.erp.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Unit of database work that runs on a connection supplied by a
 * {@link TransactionManager}.
 *
 * @param <T> result type
 */
@FunctionalInterface
public interface SqlWork<T> {

    T execute(Connection connection) throws SQLException;
}
