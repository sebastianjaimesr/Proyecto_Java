package com.littlehorizons.erp.exception;

/**
 * Technical failure while talking to the database (connection, SQL, mapping).
 *
 * <p>The message is technical and is written to the error log only; the user
 * receives a generic message from the controller layer.</p>
 */
public class DataAccessException extends ErpException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
