package com.littlehorizons.erp.model.system;

/**
 * Active configuration shown in the "Configuración y estado del sistema" menu.
 * It deliberately has no password field.
 *
 * @param configFile      configuration file in use
 * @param jdbcUrl         JDBC URL without credentials
 * @param databaseUser    MySQL user name
 * @param errorLogFile    location of error_log.txt
 * @param consoleEncoding charset used for console input and output
 * @param javaVersion     Java runtime version
 */
public record ConfigurationSummary(
        String configFile,
        String jdbcUrl,
        String databaseUser,
        String errorLogFile,
        String consoleEncoding,
        String javaVersion) {
}
