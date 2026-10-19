package com.littlehorizons.erp.model.finance;

import com.littlehorizons.erp.model.common.DbEnum;

/**
 * Métodos de pago ({@code pagos.metodo_pago}).
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

    /** Convierte el valor guardado en MySQL en la constante del enum. */
    public static PaymentMethod fromDbValue(String value) {
        return DbEnum.fromDbValue(PaymentMethod.class, value);
    }
}
