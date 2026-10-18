package com.littlehorizons.erp.model.finance;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Payment methods ({@code pagos.metodo_pago}).
 */
public enum PaymentMethod implements DbEnum {

    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DEBIT_CARD("Tarjeta Débito"),
    CREDIT_CARD("Tarjeta Crédito"),
    CHECK("Cheque");

    private final String dbValue;

    PaymentMethod(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    /** Value stored in MySQL to enum constant. */
    public static PaymentMethod fromDbValue(String value) {
        return DbEnum.fromDbValue(PaymentMethod.class, value);
    }
}
