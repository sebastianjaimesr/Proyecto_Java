package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.model.common.DbEnum;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ayudas pequeñas compartidas por los DAO JDBC: asignar parámetros que pueden ser null,
 * convertir tipos y mapear resultados. Cada sentencia sigue siendo un
 * {@link PreparedStatement} cerrado con try-with-resources.
 */
final class JdbcSupport {

    private JdbcSupport() {
    }

    /** Convierte una fila de un ResultSet en un objeto. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    /** Asigna los parámetros de una sentencia. */
    @FunctionalInterface
    interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    static final Binder NO_PARAMETERS = statement -> { };

    /**
     * Ejecuta un SELECT y convierte cada fila en un objeto con {@code mapper}; devuelve la lista completa.
     */
    static <T> List<T> queryList(java.sql.Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        }
    }

    /** Ejecuta un SELECT que devuelve como máximo una fila; {@code Optional.empty()} si no hay resultados. */
    static <T> Optional<T> queryOne(java.sql.Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapper.map(resultSet)) : Optional.empty();
            }
        }
    }

    /** Ejecuta una consulta que devuelve un solo número (por ejemplo, COUNT); 0 si no hay filas. */
    static int queryInt(java.sql.Connection connection, String sql, Binder binder) throws SQLException {
        return queryOne(connection, sql, binder, resultSet -> resultSet.getInt(1)).orElse(0);
    }

    /** Devuelve true si la consulta encuentra al menos una fila. */
    static boolean exists(java.sql.Connection connection, String sql, Binder binder) throws SQLException {
        return queryOne(connection, sql, binder, resultSet -> Boolean.TRUE).isPresent();
    }

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve cuántas filas cambió. */
    static int update(java.sql.Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        }
    }

    /** Ejecuta un INSERT y devuelve la clave AUTO_INCREMENT generada. */
    static int insert(java.sql.Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The insert did not return a generated key");
                }
                return keys.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------ parámetros

    /** Asigna un texto al parámetro; si es null guarda NULL en la base de datos. */
    static void setString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    /** Asigna un entero al parámetro; si es null guarda NULL en la base de datos. */
    static void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    /** Asigna una fecha (LocalDate) al parámetro; si es null guarda NULL en la base de datos. */
    static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    /** Asigna una hora (LocalTime) al parámetro. */
    static void setTime(PreparedStatement statement, int index, LocalTime value) throws SQLException {
        statement.setTime(index, Time.valueOf(value));
    }

    /** Asigna una fecha y hora (LocalDateTime) al parámetro. */
    static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        statement.setTimestamp(index, Timestamp.valueOf(value));
    }

    /** Asigna un decimal (BigDecimal, usado para dinero) al parámetro; si es null guarda NULL. */
    static void setDecimal(PreparedStatement statement, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    /** Asigna el valor en español de un enum ({@link DbEnum#dbValue()}); si es null guarda NULL. */
    static void setEnum(PreparedStatement statement, int index, DbEnum value) throws SQLException {
        setString(statement, index, value == null ? null : value.dbValue());
    }

    // --------------------------------------------------------------- resultados

    /** Lee un entero que puede ser NULL: JDBC devuelve 0 para NULL, por eso se revisa {@code wasNull()}. */
    static Integer getInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    /** Lee una fecha y la convierte a LocalDate; null si la columna es NULL. */
    static LocalDate getDate(ResultSet resultSet, String column) throws SQLException {
        Date value = resultSet.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    /** Lee una hora y la convierte a LocalTime; null si la columna es NULL. */
    static LocalTime getTime(ResultSet resultSet, String column) throws SQLException {
        Time value = resultSet.getTime(column);
        return value == null ? null : value.toLocalTime();
    }

    /** Lee una fecha y hora y la convierte a LocalDateTime; null si la columna es NULL. */
    static LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    /** Escapa los comodines de LIKE (% y _) en el texto del usuario y lo envuelve como patrón "contiene". */
    static String containsPattern(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
