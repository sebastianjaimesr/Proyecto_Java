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
 * Generates the <strong>simulated</strong> CUFE printed on the invoices.
 *
 * <p>Documented input (fields joined with "|"):</p>
 * <pre>
 * invoice number | configured NIT | ISO date and time (yyyy-MM-dd'T'HH:mm:ss) | total with 2 decimals
 * example: FE-000123|900000000-0|2026-10-15T14:30:00|350000.00
 * </pre>
 * <p>The value is the SHA-384 hash of that text in lower-case hexadecimal (96
 * characters, fits in {@code facturas.cufe VARCHAR(100)}). It only imitates
 * the shape of a CUFE: the real DIAN algorithm uses more fields and a
 * technical key assigned by the DIAN, which this project does not have.</p>
 */
public final class CufeGenerator {

    public static final String ALGORITHM = "SHA-384";
    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private CufeGenerator() {
    }

    /** Text that is hashed; exposed so the README and tests can show it. */
    public static String input(String invoiceNumber, String nit, LocalDateTime issuedAt, BigDecimal total) {
        return String.join("|", invoiceNumber, nit, ISO_SECONDS.format(issuedAt),
                total.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    public static String generate(String invoiceNumber, String nit, LocalDateTime issuedAt, BigDecimal total) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input(invoiceNumber, nit, issuedAt, total).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Every Java SE runtime must provide SHA-384, so this means a broken runtime.
            throw new IllegalStateException("SHA-384 is not available in this Java runtime", e);
        }
    }
}
