package com.littlehorizons.erp.exception;

/**
 * The club has no free seats. A subclass of {@link BusinessRuleException} so
 * callers that only care about "rejected" keep working, while the bulk
 * enrollment can send the student to the waiting list.
 */
public class ClubFullException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;

    public ClubFullException(String userMessage) {
        super(userMessage);
    }
}
