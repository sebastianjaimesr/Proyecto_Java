package com.littlehorizons.erp.exception;

/**
 * Tipo base de todas las excepciones que lanza la aplicación.
 *
 * <p>Es no verificada (unchecked) para que las firmas de DAOs y servicios sigan siendo legibles; la
 * capa de controladores es la frontera donde estas excepciones se traducen a
 * mensajes para el usuario.</p>
 */
public abstract class ErpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ErpException(String message) {
        super(message);
    }

    protected ErpException(String message, Throwable cause) {
        super(message, cause);
    }
}
