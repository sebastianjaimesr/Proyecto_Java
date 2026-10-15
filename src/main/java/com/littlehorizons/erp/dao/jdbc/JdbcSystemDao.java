package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.SystemDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.system.DatabaseInfo;
import com.littlehorizons.erp.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC implementation of {@link SystemDao} for MySQL.
 */
public class JdbcSystemDao implements SystemDao {

    private static final String SQL_SERVER_INFO = "SELECT VERSION(), DATABASE()";
    private static final String SQL_TABLE_NAMES =
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' "
                    + "ORDER BY TABLE_NAME";

    private final ConnectionProvider connectionProvider;

    public JdbcSystemDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    @Override
    public DatabaseInfo findDatabaseInfo() {
        try (Connection connection = connectionProvider.getConnection()) {
            String serverVersion;
            String databaseName;
            try (PreparedStatement statement = connection.prepareStatement(SQL_SERVER_INFO);
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                serverVersion = resultSet.getString(1);
                databaseName = resultSet.getString(2);
            }
            return new DatabaseInfo(serverVersion, databaseName, findTableNames(connection, databaseName));
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the database server information", e);
        }
    }

    private static List<String> findTableNames(Connection connection, String databaseName) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SQL_TABLE_NAMES)) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString(1));
                }
            }
        }
        return tableNames;
    }
}
