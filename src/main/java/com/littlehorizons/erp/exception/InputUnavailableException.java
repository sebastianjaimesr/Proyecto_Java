package com.littlehorizons.erp.exception;

/**
 * The console input stream was closed (end of input), so no more answers can
 * be read. The application reacts by shutting down in an orderly way.
 */
public class InputUnavailableException extends ErpException {

    private static final long serialVersionUID = 1L;

    public InputUnavailableException() {
        super("Console input is no longer available");
    }
}
