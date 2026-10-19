package com.littlehorizons.erp.exception;

/**
 * Entrada inválida del usuario. El mensaje está en español porque se muestra
 * directamente al usuario para que corrija el valor y vuelva a intentar.
 */
public class ValidationException extends ErpException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String userMessage) {
        super(userMessage);
    }
}
