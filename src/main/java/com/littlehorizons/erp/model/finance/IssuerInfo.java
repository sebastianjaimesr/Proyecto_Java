package com.littlehorizons.erp.model.finance;

/**
 * Datos legales del colegio que se imprimen en las facturas. Los valores vienen de
 * config.properties; los que faltan se muestran como "PENDIENTE DE CONFIGURACIÓN"
 * porque el proyecto no debe inventar un NIT ni una dirección reales.
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
