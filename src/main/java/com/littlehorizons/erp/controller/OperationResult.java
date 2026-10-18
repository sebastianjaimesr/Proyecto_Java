package com.littlehorizons.erp.controller;

import java.util.Optional;

/**
 * Outcome of a controller operation, ready to be shown by a view.
 *
 * @param success              whether the operation finished correctly
 * @param message              message for the user, in Spanish
 * @param data                 result of the operation; {@code null} when it failed
 * @param confirmationRequired the operation failed only because it would
 *                             replace existing data and can be retried after
 *                             the user confirms
 * @param <T>                  type of the result
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
