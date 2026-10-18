package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;

/**
 * Comparison between what a student should pay for a concept and what was
 * paid. For recurring concepts the comparison is per month of payment; for
 * the others it is per academic year.
 *
 * @param month 1-12 for recurring concepts, {@code null} otherwise
 */
public record ChargeStatus(
        int conceptId,
        String conceptCode,
        String conceptName,
        int academicYear,
        Integer month,
        BigDecimal expected,
        BigDecimal paid,
        BigDecimal balance,
        State state) {

    /** Payment state shown to the user. */
    public enum State {
        PAID("Pagado"),
        PARTIAL("Pago parcial"),
        OVERPAID("Pagado con excedente");

        private final String label;

        State(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
