package com.littlehorizons.erp.exception;

/**
 * Configuración local faltante o inválida (config.properties). El mensaje está en
 * español y explica cómo corregir el archivo; nunca contiene credenciales.
 */
public class ConfigurationException extends ErpException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String userMessage) {
        super(userMessage);
    }

    public ConfigurationException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
