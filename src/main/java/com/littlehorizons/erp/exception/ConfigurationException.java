package com.littlehorizons.erp.exception;

/**
 * Missing or invalid local configuration (config.properties). The message is in
 * Spanish and explains how to fix the file; it never contains credentials.
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
