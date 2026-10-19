package com.littlehorizons.erp.exception;

/**
 * La operación reemplazaría datos históricos existentes (una nota o una
 * marca de asistencia). La vista le pregunta al usuario y reintenta con la corrección
 * permitida explícitamente.
 */
public class ConfirmationRequiredException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;

    public ConfirmationRequiredException(String userMessage) {
        super(userMessage);
    }
}
