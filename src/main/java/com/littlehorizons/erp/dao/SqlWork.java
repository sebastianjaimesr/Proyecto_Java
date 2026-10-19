package com.littlehorizons.erp.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Unidad de trabajo de base de datos que se ejecuta sobre una conexión entregada por un
 * {@link TransactionManager}.
 *
 * @param <T> tipo del resultado
 */
@FunctionalInterface
public interface SqlWork<T> {

    T execute(Connection connection) throws SQLException;
}
