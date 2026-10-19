package com.littlehorizons.erp.model.finance;

import java.math.BigDecimal;

/**
 * Comparación entre lo que un estudiante debe pagar por un concepto y lo que
 * pagó. En los conceptos recurrentes la comparación es por mes de pago; en
 * los demás, por año académico.
 *
 * @param month 1-12 en conceptos recurrentes, {@code null} en los demás
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

    /** Estado de pago que se muestra al usuario. */
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
