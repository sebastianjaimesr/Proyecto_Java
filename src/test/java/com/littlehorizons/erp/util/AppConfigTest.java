package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsUtf8FileAndTrimsValues() throws IOException {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "db.host = localhost \nschool.name=Colegio Internacional Little Horizons — Bogotá\n",
                StandardCharsets.UTF_8);

        AppConfig config = AppConfig.load(file);

        assertEquals("localhost", config.getRequired("db.host"));
        assertEquals("Colegio Internacional Little Horizons — Bogotá", config.getRequired("school.name"));
        assertEquals(file.toAbsolutePath().normalize().toString(), config.getSource());
    }

    @Test
    void missingFileExplainsHowToCreateIt() {
        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> AppConfig.load(tempDir.resolve("no-existe.properties")));
        assertTrue(error.getMessage().contains("config.example.properties"));
    }

    @Test
    void missingRequiredKeyIsReported() {
        AppConfig config = AppConfig.fromProperties(new Properties());
        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> config.getRequired("db.user"));
        assertTrue(error.getMessage().contains("db.user"));
    }

    @Test
    void optionalAndIntegerValuesUseDefaults() {
        Properties properties = new Properties();
        properties.setProperty("db.port", "3307");
        properties.setProperty("blank", "  ");
        AppConfig config = AppConfig.fromProperties(properties);

        assertEquals(3307, config.getInt("db.port", 3306));
        assertEquals(3306, config.getInt("missing", 3306));
        assertEquals("logs", config.getOptional("blank", "logs"));
    }

    @Test
    void nonNumericIntegerIsRejected() {
        Properties properties = new Properties();
        properties.setProperty("db.port", "tres mil");
        AppConfig config = AppConfig.fromProperties(properties);
        assertThrows(ConfigurationException.class, () -> config.getInt("db.port", 3306));
    }
}
