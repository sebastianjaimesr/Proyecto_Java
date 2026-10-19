package com.littlehorizons.erp.exception;

/**
 * Falla técnica al comunicarse con la base de datos (conexión, SQL, mapeo).
 *
 * <p>El mensaje es técnico y solo se escribe en el registro de errores; el usuario
 * recibe un mensaje genérico desde la capa de controladores.</p>
 */
public class DataAccessException extends ErpException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
