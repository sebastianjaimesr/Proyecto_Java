package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC connection provider for MySQL built from {@link AppConfig}.
 *
 * <p>Singleton by design: the connection settings are resolved once. It does
 * <strong>not</strong> keep a shared open connection; every call to
 * {@link #getConnection()} opens a new one, which keeps transactions isolated
 * and makes the class safe to use from several threads.</p>
 */
public final class DatabaseConnection implements ConnectionProvider {

    static final String KEY_HOST = "db.host";
    static final String KEY_PORT = "db.port";
    static final String KEY_NAME = "db.name";
    static final String KEY_USER = "db.user";
    static final String KEY_PASSWORD = "db.password";
    static final String KEY_PARAMS = "db.params";

    private static final int DEFAULT_PORT = 3306;
    private static final int MAX_PORT = 65_535;

    private static volatile DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private final String databaseName;

    private DatabaseConnection(AppConfig config) {
        String host = config.getRequired(KEY_HOST);
        int port = config.getInt(KEY_PORT, DEFAULT_PORT);
        if (port < 1 || port > MAX_PORT) {
            throw new ConfigurationException("La propiedad '" + KEY_PORT
                    + "' debe estar entre 1 y " + MAX_PORT + ".");
        }
        this.databaseName = config.getRequired(KEY_NAME);
        this.user = config.getRequired(KEY_USER);
        // An empty password is allowed for local installations without one.
        this.password = config.getOptional(KEY_PASSWORD, "");
        String params = config.getOptional(KEY_PARAMS, "");
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + (params.isEmpty() ? "" : "?" + params);
    }

    /** Shared provider built from {@link AppConfig#getInstance()}. */
    public static DatabaseConnection getInstance() {
        DatabaseConnection result = instance;
        if (result == null) {
            synchronized (DatabaseConnection.class) {
                result = instance;
                if (result == null) {
                    result = new DatabaseConnection(AppConfig.getInstance());
                    instance = result;
                }
            }
        }
        return result;
    }

    /** Independent provider for a specific configuration; used by tests. */
    public static DatabaseConnection fromConfig(AppConfig config) {
        return new DatabaseConnection(config);
    }

    @Override
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // The URL and user name are safe to log; the password is never included.
            throw new DataAccessException("Could not open a connection to " + url
                    + " as user '" + user + "' (SQLState " + e.getSQLState() + ")", e);
        }
    }

    /** JDBC URL without credentials, safe to show or log. */
    public String getSafeUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
