package com.littlehorizons.erp.exception;

/**
 * Se cerró la entrada de la consola (fin de entrada), así que no se pueden leer más
 * respuestas. La aplicación reacciona cerrándose de forma ordenada.
 */
public class InputUnavailableException extends ErpException {

    private static final long serialVersionUID = 1L;

    public InputUnavailableException() {
        super("Console input is no longer available");
    }
}
