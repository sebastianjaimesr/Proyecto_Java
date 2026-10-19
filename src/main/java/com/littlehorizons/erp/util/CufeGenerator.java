package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Genera el CUFE <strong>simulado</strong> que se imprime en las facturas.
 *
 * <p>Entrada documentada (campos unidos con "|"):</p>
 * <pre>
 * numero_factura | NIT configurado | fecha y hora ISO (yyyy-MM-dd'T'HH:mm:ss) | total con 2 decimales
 * ejemplo: FE-000123|900000000-0|2026-10-15T14:30:00|350000.00
 * </pre>
 * <p>El valor es el hash SHA-384 de ese texto en hexadecimal en minúsculas (96
 * caracteres, cabe en {@code facturas.cufe VARCHAR(100)}). Solo imita
 * la forma de un CUFE: el algoritmo real de la DIAN usa más campos y una
 * clave técnica asignada por la DIAN, que este proyecto no tiene.</p>
 */
public final class CufeGenerator {

    public static final String ALGORITHM = "SHA-384";
    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private CufeGenerator() {
    }

    /** Texto al que se le aplica el hash; es público para que el README y las pruebas lo puedan mostrar. */
    public static String input(String invoiceNumber, String nit, LocalDateTime issuedAt, BigDecimal total) {
        return String.join("|", invoiceNumber, nit, ISO_SECONDS.format(issuedAt),
                total.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /** Calcula el CUFE simulado: hash SHA-384 del texto de entrada, en hexadecimal. */
    public static String generate(String invoiceNumber, String nit, LocalDateTime issuedAt, BigDecimal total) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input(invoiceNumber, nit, issuedAt, total).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Todo entorno Java SE debe incluir SHA-384, así que esto significa un entorno dañado.
            throw new IllegalStateException("SHA-384 is not available in this Java runtime", e);
        }
    }
}
