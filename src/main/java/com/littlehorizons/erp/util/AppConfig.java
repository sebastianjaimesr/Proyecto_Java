package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ConfigurationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Read-only access to the local configuration file ({@code config.properties}).
 *
 * <p>Singleton: the file is read once per execution and shared by every layer.
 * Tests and alternative setups can build independent instances with
 * {@link #fromProperties(Properties)} or {@link #load(Path)}.</p>
 *
 * <p>The file location is {@code config.properties} in the working directory,
 * or the path given by the {@code app.config} system property.</p>
 */
public final class AppConfig {

    public static final String CONFIG_PATH_PROPERTY = "app.config";
    public static final String DEFAULT_CONFIG_FILE = "config.properties";

    private static volatile AppConfig instance;

    private final Properties properties;
    private final String source;

    private AppConfig(Properties properties, String source) {
        this.properties = new Properties();
        this.properties.putAll(properties);
        this.source = source;
    }

    /**
     * Returns the shared configuration, loading it on first use.
     *
     * @throws ConfigurationException if the file is missing or unreadable
     */
    public static AppConfig getInstance() {
        AppConfig result = instance;
        if (result == null) {
            synchronized (AppConfig.class) {
                result = instance;
                if (result == null) {
                    result = load(resolveConfigPath());
                    instance = result;
                }
            }
        }
        return result;
    }

    /** Path used by {@link #getInstance()}. */
    public static Path resolveConfigPath() {
        String customPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (customPath != null && !customPath.isBlank()) {
            return Paths.get(customPath.trim());
        }
        return Paths.get(DEFAULT_CONFIG_FILE);
    }

    /**
     * Loads a configuration file encoded in UTF-8.
     *
     * @throws ConfigurationException if the file is missing or unreadable
     */
    public static AppConfig load(Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolutePath)) {
            throw new ConfigurationException("No se encontró el archivo de configuración "
                    + absolutePath + ". Copie config.example.properties como config.properties "
                    + "y complete los datos de conexión.");
        }
        Properties loaded = new Properties();
        try (Reader reader = Files.newBufferedReader(absolutePath, StandardCharsets.UTF_8)) {
            loaded.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            throw new ConfigurationException("No se pudo leer el archivo de configuración "
                    + absolutePath + ". Verifique que esté guardado en UTF-8.", e);
        }
        return new AppConfig(loaded, absolutePath.toString());
    }

    /** Builds a configuration from memory; used by tests. */
    public static AppConfig fromProperties(Properties properties) {
        return new AppConfig(properties, "(memoria)");
    }

    /**
     * Returns a mandatory value without surrounding spaces.
     *
     * @throws ConfigurationException if the key is missing or blank
     */
    public String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Falta la propiedad obligatoria '" + key
                    + "' en el archivo de configuración " + source + ".");
        }
        return value.trim();
    }

    /** Returns the value, or {@code defaultValue} when it is missing or blank. */
    public String getOptional(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /**
     * Returns an integer value, or {@code defaultValue} when it is missing.
     *
     * @throws ConfigurationException if the value is not an integer
     */
    public int getInt(String key, int defaultValue) {
        String value = getOptional(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("La propiedad '" + key
                    + "' debe ser un número entero en el archivo de configuración " + source + ".", e);
        }
    }

    /** Human readable origin of the values (file path or memory). */
    public String getSource() {
        return source;
    }
}
