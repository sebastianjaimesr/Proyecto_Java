package com.littlehorizons.erp.controller;

import java.util.Optional;

/**
 * Resultado de una operación del controlador, listo para que lo muestre una vista.
 *
 * @param success              si la operación terminó correctamente
 * @param message              mensaje para el usuario, en español
 * @param data                 resultado de la operación; {@code null} si falló
 * @param confirmationRequired la operación falló solo porque reemplazaría
 *                             datos existentes; se puede reintentar después
 *                             de que el usuario confirme
 * @param <T>                  tipo del resultado
 */
public record OperationResult<T>(boolean success, String message, T data, boolean confirmationRequired) {

    public static <T> OperationResult<T> ok(String message, T data) {
        return new OperationResult<>(true, message, data, false);
    }

    public static <T> OperationResult<T> failure(String message) {
        return new OperationResult<>(false, message, null, false);
    }

    public static <T> OperationResult<T> needsConfirmation(String message) {
        return new OperationResult<>(false, message, null, true);
    }

    public Optional<T> dataOptional() {
        return Optional.ofNullable(data);
    }
}
