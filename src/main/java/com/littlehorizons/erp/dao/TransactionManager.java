package com.littlehorizons.erp.dao;

/**
 * Ejecuta trabajo de base de datos sobre su propia conexión.
 *
 * <p>Los servicios lo usan para agrupar varias llamadas a DAOs: todo lo que está dentro de
 * {@link #inTransaction(SqlWork)} se confirma junto (commit) o se deshace junto
 * (rollback). Los métodos DAO reciben la conexión como parámetro, así el límite
 * de la transacción siempre es visible en el código del servicio.</p>
 */
public interface TransactionManager {

    /**
     * Ejecuta {@code work} en una transacción: commit cuando termina bien, rollback cuando
     * lanza una excepción.
     *
     * @throws com.littlehorizons.erp.exception.DataAccessException para errores SQL
     * @throws com.littlehorizons.erp.exception.BusinessRuleException cuando una
     *         restricción única o de clave foránea rechaza los datos
     */
    <T> T inTransaction(SqlWork<T> work);

    /** Ejecuta trabajo de solo lectura con auto-commit sobre una conexión nueva. */
    <T> T read(SqlWork<T> work);

    /** Atajo para trabajo transaccional que no devuelve resultado. */
    default void inTransactionVoid(SqlVoidWork work) {
        inTransaction(connection -> {
            work.execute(connection);
            return null;
        });
    }

    /** Trabajo sin resultado. */
    @FunctionalInterface
    interface SqlVoidWork {
        void execute(java.sql.Connection connection) throws java.sql.SQLException;
    }
}
