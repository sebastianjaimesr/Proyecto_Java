package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.exception.ConfirmationRequiredException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.InputUnavailableException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.util.AppLogger;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Traduce las excepciones de la capa de servicios a {@link OperationResult}.
 *
 * <p>Los mensajes de validación y de reglas de negocio ya están escritos para el usuario.
 * Los errores técnicos se registran con su stack trace y se reemplazan por un mensaje
 * genérico, así los detalles de SQL y los stack traces nunca llegan a la consola.</p>
 */
public abstract class BaseController {

    static final String DATABASE_ERROR_MESSAGE =
            "No fue posible comunicarse con la base de datos. Verifique que MySQL esté en ejecución "
                    + "y los datos de config.properties. El detalle técnico quedó en error_log.txt.";
    static final String UNEXPECTED_ERROR_MESSAGE =
            "Ocurrió un error inesperado. El detalle técnico quedó en error_log.txt.";

    /** Ejecuta {@code action} y construye el mensaje de éxito a partir de su resultado. */
    protected <T> OperationResult<T> execute(Supplier<T> action, Function<T, String> successMessage) {
        try {
            T result = action.get();
            return OperationResult.ok(successMessage.apply(result), result);
        } catch (ConfirmationRequiredException e) {
            return OperationResult.needsConfirmation(e.getMessage());
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

    /** Igual que {@link #execute(Supplier, Function)} pero con un mensaje de éxito fijo. */
    protected <T> OperationResult<T> execute(Supplier<T> action, String successMessage) {
        return execute(action, result -> successMessage);
    }
}
