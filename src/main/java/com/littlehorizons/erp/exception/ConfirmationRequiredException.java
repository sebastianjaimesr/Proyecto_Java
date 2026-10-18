package com.littlehorizons.erp.exception;

/**
 * The operation would replace existing historical data (a grade or an
 * attendance mark). The view asks the user and retries with the correction
 * explicitly allowed.
 */
public class ConfirmationRequiredException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;

    public ConfirmationRequiredException(String userMessage) {
        super(userMessage);
    }
}
