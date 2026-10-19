package com.littlehorizons.erp.service;

import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.model.finance.IssuerInfo;
import com.littlehorizons.erp.util.AppConfig;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuración financiera leída de config.properties: datos legales del emisor, carpeta
 * de facturas, IVA por concepto y conceptos que todo matriculado debe pagar.
 */
public record FinanceSettings(
        IssuerInfo issuer,
        Path invoicesDirectory,
        Map<String, BigDecimal> taxPercentByConcept,
        Set<String> mandatoryConcepts) {

    public static final String DEFAULT_BUSINESS_NAME = "Colegio Internacional Little Horizons S.A.S.";
    public static final String DEFAULT_RESOLUTION = "SIMULADA - Resolución de ejemplo No. 00000000000000 del "
            + "2026-01-01, prefijo FE, rango FE-000001 a FE-999999. Sin validez ante la DIAN.";
    private static final String TAX_PREFIX = "finance.tax.percent.";

    public FinanceSettings {
        taxPercentByConcept = Map.copyOf(taxPercentByConcept);
        mandatoryConcepts = Set.copyOf(mandatoryConcepts);
    }

    public static FinanceSettings from(AppConfig config) {
        IssuerInfo issuer = new IssuerInfo(
                config.getOptional("issuer.name", DEFAULT_BUSINESS_NAME),
                config.getOptional("issuer.nit", IssuerInfo.PENDING),
                config.getOptional("issuer.address", IssuerInfo.PENDING),
                config.getOptional("issuer.phone", IssuerInfo.PENDING),
                config.getOptional("issuer.email", IssuerInfo.PENDING),
                config.getOptional("issuer.resolution", DEFAULT_RESOLUTION));
        Map<String, BigDecimal> taxes = new java.util.HashMap<>();
        config.withPrefix(TAX_PREFIX).forEach((code, raw) ->
                taxes.put(code.toUpperCase(Locale.ROOT), parsePercent(TAX_PREFIX + code, raw)));
        Set<String> mandatory = Arrays.stream(config.getOptional("finance.mandatory.concepts", "MAT,PENSION").split(","))
                .map(code -> code.strip().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toSet());
        return new FinanceSettings(issuer, Paths.get(config.getOptional("invoices.dir", "invoices")), taxes, mandatory);
    }

    /** Configuración por defecto para pruebas: datos del emisor pendientes, sin IVA, MAT y PENSION obligatorios. */
    public static FinanceSettings defaults(Path invoicesDirectory) {
        return new FinanceSettings(new IssuerInfo(DEFAULT_BUSINESS_NAME, IssuerInfo.PENDING, IssuerInfo.PENDING,
                IssuerInfo.PENDING, IssuerInfo.PENDING, DEFAULT_RESOLUTION), invoicesDirectory, Map.of(),
                Set.of("MAT", "PENSION"));
    }

    private static BigDecimal parsePercent(String key, String raw) {
        try {
            BigDecimal value = new BigDecimal(raw.strip());
            if (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ConfigurationException("La propiedad " + key + " debe estar entre 0 y 100.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ConfigurationException("La propiedad " + key + " debe ser un número (porcentaje de IVA).", e);
        }
    }
}
