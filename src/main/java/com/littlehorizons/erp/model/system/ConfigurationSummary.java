package com.littlehorizons.erp.model.system;

/**
 * Configuración activa que se muestra en el menú "Configuración y estado del sistema".
 * A propósito no tiene campo de contraseña.
 *
 * @param configFile      archivo de configuración en uso
 * @param jdbcUrl         URL JDBC sin credenciales
 * @param databaseUser    nombre de usuario de MySQL
 * @param errorLogFile    ubicación de error_log.txt
 * @param consoleEncoding codificación usada para la entrada y salida de la consola
 * @param javaVersion     versión de Java en ejecución
 */
public record ConfigurationSummary(
        String configFile,
        String jdbcUrl,
        String databaseUser,
        String errorLogFile,
        String consoleEncoding,
        String javaVersion) {
}
