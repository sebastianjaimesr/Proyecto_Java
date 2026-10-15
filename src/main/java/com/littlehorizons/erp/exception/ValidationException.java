package com.littlehorizons.erp.exception;

/**
 * Invalid user input. The message is written in Spanish because it is shown
 * directly to the user so they can correct the value and try again.
 */
public class ValidationException extends ErpException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String userMessage) {
        super(userMessage);
    }
}
