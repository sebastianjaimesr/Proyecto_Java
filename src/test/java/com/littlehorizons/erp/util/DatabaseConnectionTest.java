package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {

    private static final String SECRET = "NoDebeAparecer#2026";

    private static Properties validProperties() {
        Properties properties = new Properties();
        properties.setProperty("db.host", "127.0.0.1");
        properties.setProperty("db.port", "1");
        properties.setProperty("db.name", "erp_little_horizons");
        properties.setProperty("db.user", "usuario_prueba");
        properties.setProperty("db.password", SECRET);
        properties.setProperty("db.params", "connectTimeout=2000");
        return properties;
    }

    @Test
    void urlIsBuiltFromConfigurationWithoutPassword() {
        DatabaseConnection connection = DatabaseConnection.fromConfig(AppConfig.fromProperties(validProperties()));

        assertEquals("jdbc:mysql://127.0.0.1:1/erp_little_horizons?connectTimeout=2000", connection.getSafeUrl());
        assertEquals("usuario_prueba", connection.getUser());
        assertFalse(connection.getSafeUrl().contains(SECRET));
    }

    @Test
    void unreachableServerRaisesDataAccessExceptionWithoutPassword() {
        DatabaseConnection connection = DatabaseConnection.fromConfig(AppConfig.fromProperties(validProperties()));

        DataAccessException error = assertTimeoutPreemptively(Duration.ofSeconds(15),
                () -> assertThrows(DataAccessException.class, connection::getConnection));

        assertTrue(error.getMessage().contains("127.0.0.1:1"));
        assertFalse(error.getMessage().contains(SECRET), "the password must never be part of the message");
    }

    @Test
    void invalidPortIsRejected() {
        Properties properties = validProperties();
        properties.setProperty("db.port", "70000");
        assertThrows(ConfigurationException.class,
                () -> DatabaseConnection.fromConfig(AppConfig.fromProperties(properties)));
    }

    @Test
    void missingUserIsRejected() {
        Properties properties = validProperties();
        properties.remove("db.user");
        assertThrows(ConfigurationException.class,
                () -> DatabaseConnection.fromConfig(AppConfig.fromProperties(properties)));
    }
}
