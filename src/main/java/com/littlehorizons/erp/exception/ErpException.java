package com.littlehorizons.erp.exception;

/**
 * Base type for every exception raised by the application.
 *
 * <p>It is unchecked so that the DAO and service signatures stay readable; the
 * controller layer is the boundary where these exceptions are translated into
 * messages for the user.</p>
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
