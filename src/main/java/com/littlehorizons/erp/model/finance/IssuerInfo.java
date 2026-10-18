package com.littlehorizons.erp.model.finance;

/**
 * Legal data of the school printed on invoices. Values come from
 * config.properties; missing ones are shown as "PENDIENTE DE CONFIGURACIÓN"
 * because the project must not invent a real NIT or address.
 */
public record IssuerInfo(
        String businessName,
        String nit,
        String address,
        String phone,
        String email,
        String resolutionText) {

    public static final String PENDING = "PENDIENTE DE CONFIGURACIÓN";
}
