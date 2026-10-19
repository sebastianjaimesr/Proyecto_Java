package com.littlehorizons.erp.exception;

/**
 * Una solicitud válida que rompe una regla del colegio (por ejemplo, un club lleno o un
 * recurso no disponible). El mensaje está en español y se muestra al usuario.
 */
public class BusinessRuleException extends ErpException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String userMessage) {
        super(userMessage);
    }
}
