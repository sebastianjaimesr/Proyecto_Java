package com.littlehorizons.erp.controller;

import java.util.Optional;

/**
 * Outcome of a controller operation, ready to be shown by a view.
 *
 * @param success whether the operation finished correctly
 * @param message message for the user, in Spanish
 * @param data    result of the operation; {@code null} when it failed
 * @param <T>     type of the result
 */
public record OperationResult<T>(boolean success, String message, T data) {

    public static <T> OperationResult<T> ok(String message, T data) {
        return new OperationResult<>(true, message, data);
    }

    public static <T> OperationResult<T> failure(String message) {
        return new OperationResult<>(false, message, null);
    }

    public Optional<T> dataOptional() {
        return Optional.ofNullable(data);
    }
}
