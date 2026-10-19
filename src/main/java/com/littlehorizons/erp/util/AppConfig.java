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
 * Acceso de solo lectura al archivo de configuración local ({@code config.properties}).
 *
 * <p>Singleton: el archivo se lee una vez por ejecución y lo comparten todas las capas.
 * Las pruebas y otras configuraciones pueden crear instancias independientes con
 * {@link #fromProperties(Properties)} o {@link #load(Path)}.</p>
 *
 * <p>El archivo es {@code config.properties} en la carpeta de trabajo,
 * o la ruta indicada en la propiedad del sistema {@code app.config}.</p>
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
     * Devuelve la configuración compartida y la carga la primera vez que se usa.
     *
     * @throws ConfigurationException si el archivo falta o no se puede leer
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

    /** Ruta que usa {@link #getInstance()}. */
    public static Path resolveConfigPath() {
        String customPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (customPath != null && !customPath.isBlank()) {
            return Paths.get(customPath.trim());
        }
        return Paths.get(DEFAULT_CONFIG_FILE);
    }

    /**
     * Carga un archivo de configuración en UTF-8.
     *
     * @throws ConfigurationException si el archivo falta o no se puede leer
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

    /** Construye una configuración en memoria; la usan las pruebas. */
    public static AppConfig fromProperties(Properties properties) {
        return new AppConfig(properties, "(memoria)");
    }

    /**
     * Devuelve un valor obligatorio sin espacios alrededor.
     *
     * @throws ConfigurationException si la clave falta o está vacía
     */
    public String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Falta la propiedad obligatoria '" + key
                    + "' en el archivo de configuración " + source + ".");
        }
        return value.trim();
    }

    /** Devuelve el valor, o {@code defaultValue} si falta o está vacío. */
    public String getOptional(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /**
     * Devuelve un valor entero, o {@code defaultValue} si falta.
     *
     * @throws ConfigurationException si el valor no es un entero
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

    /** Todas las propiedades cuya clave empieza con {@code prefix}, indexadas por el resto del nombre. */
    public java.util.Map<String, String> withPrefix(String prefix) {
        java.util.Map<String, String> values = new java.util.TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix) && key.length() > prefix.length()) {
                values.put(key.substring(prefix.length()), properties.getProperty(key).trim());
            }
        }
        return values;
    }

    /** Origen legible de los valores (ruta del archivo o memoria). */
    public String getSource() {
        return source;
    }
}
