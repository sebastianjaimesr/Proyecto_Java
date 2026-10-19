package com.littlehorizons.erp.util;

import java.sql.Connection;

/**
 * Fuente de conexiones JDBC.
 *
 * <p>Los DAO dependen de esta abstracción y no de {@link DatabaseConnection},
 * así las pruebas pueden entregar otra implementación (Inversión de dependencias).</p>
 */
@FunctionalInterface
public interface ConnectionProvider {

    /**
     * Abre una conexión nueva. Cada llamada devuelve una conexión distinta que
     * pertenece a quien la pide y debe cerrarla, idealmente con try-with-resources; las conexiones
     * nunca se comparten entre operaciones.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException si no se
     *         puede conectar a la base de datos
     */
    Connection getConnection();
}
