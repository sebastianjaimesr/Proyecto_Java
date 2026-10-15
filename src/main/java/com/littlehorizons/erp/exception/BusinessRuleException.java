package com.littlehorizons.erp.exception;

/**
 * A valid request that breaks a school rule (for example a full club or an
 * unavailable resource). The message is in Spanish and is shown to the user.
 */
public class BusinessRuleException extends ErpException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String userMessage) {
        super(userMessage);
    }
}
