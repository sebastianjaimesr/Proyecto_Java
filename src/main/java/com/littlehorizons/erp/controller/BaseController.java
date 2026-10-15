package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.InputUnavailableException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.util.AppLogger;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Translates exceptions from the service layer into {@link OperationResult}s.
 *
 * <p>Validation and business rule messages are already written for the user.
 * Technical errors are logged with their stack trace and replaced by a generic
 * message, so SQL details and stack traces never reach the console.</p>
 */
public abstract class BaseController {

    static final String DATABASE_ERROR_MESSAGE =
            "No fue posible comunicarse con la base de datos. Verifique que MySQL esté en ejecución "
                    + "y los datos de config.properties. El detalle técnico quedó en error_log.txt.";
    static final String UNEXPECTED_ERROR_MESSAGE =
            "Ocurrió un error inesperado. El detalle técnico quedó en error_log.txt.";

    /** Runs {@code action} and builds the success message from its result. */
    protected <T> OperationResult<T> execute(Supplier<T> action, Function<T, String> successMessage) {
        try {
            T result = action.get();
            return OperationResult.ok(successMessage.apply(result), result);
        } catch (ValidationException | BusinessRuleException | ConfigurationException e) {
            return OperationResult.failure(e.getMessage());
        } catch (DataAccessException e) {
            AppLogger.logError(getClass(), e.getMessage(), e);
            return OperationResult.failure(DATABASE_ERROR_MESSAGE);
        } catch (InputUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            AppLogger.logError(getClass(), "Unexpected error in " + getClass().getSimpleName(), e);
            return OperationResult.failure(UNEXPECTED_ERROR_MESSAGE);
        }
    }

    /** Same as {@link #execute(Supplier, Function)} with a fixed success message. */
    protected <T> OperationResult<T> execute(Supplier<T> action, String successMessage) {
        return execute(action, result -> successMessage);
    }
}
