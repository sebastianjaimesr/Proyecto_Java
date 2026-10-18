package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.finance.InvoiceDocument;
import com.littlehorizons.erp.model.finance.IssuerInfo;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulated CUFE, QR round trip and PDF generation, without a database.
 */
class InvoiceArtifactsTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 10, 15, 14, 30, 0);

    @TempDir
    Path tempDir;

    @Test
    void cufeIsSha384OfTheDocumentedInput() {
        assertEquals("FE-000123|900000000-0|2026-10-15T14:30:00|350000.00",
                CufeGenerator.input("FE-000123", "900000000-0", ISSUED_AT, new BigDecimal("350000")));

        String cufe = CufeGenerator.generate("FE-000123", "900000000-0", ISSUED_AT, new BigDecimal("350000"));

        assertEquals(96, cufe.length());
        assertTrue(cufe.matches("[0-9a-f]{96}"));
        assertEquals(cufe, CufeGenerator.generate("FE-000123", "900000000-0", ISSUED_AT, new BigDecimal("350000.00")),
                "same data, same CUFE");
        assertNotEquals(cufe, CufeGenerator.generate("FE-000124", "900000000-0", ISSUED_AT, new BigDecimal("350000")));
    }

    @Test
    void qrCanBeGeneratedAndReadBack() {
        String content = "FACTURA:FE-000123|FECHA:2026-10-15T14:30:00|TOTAL:350000.00|NIT:PENDIENTE DE CONFIGURACIÓN";

        byte[] png = QrGenerator.toPng(content, QrGenerator.DEFAULT_SIZE);

        assertTrue(png.length > 100);
        assertEquals(content, QrGenerator.read(png));
        assertThrows(IllegalArgumentException.class, () -> QrGenerator.read(new byte[] {1, 2, 3}));
    }

    @Test
    void pdfContainsTheInvoiceData() throws IOException {
        IssuerInfo issuer = new IssuerInfo("Colegio Internacional Little Horizons S.A.S.", IssuerInfo.PENDING,
                IssuerInfo.PENDING, IssuerInfo.PENDING, IssuerInfo.PENDING, "SIMULADA - resolución de ejemplo");
        String cufe = CufeGenerator.generate("FE-000123", issuer.nit(), ISSUED_AT, new BigDecimal("350000"));
        InvoiceDocument document = new InvoiceDocument("FE-000123", ISSUED_AT, issuer, "Carlos Pérez Gómez",
                "CC 52000101", "Calle 10 # 5-20, Bogotá", "Sofía Pérez", "RC-2026-000045", "Transferencia",
                List.of(new InvoiceDocument.Line("Pensión Mensual - año 2026", 1, new BigDecimal("350000.00"),
                        new BigDecimal("350000.00"))),
                new BigDecimal("350000.00"), BigDecimal.ZERO, new BigDecimal("0.00"), new BigDecimal("350000.00"),
                cufe, "FACTURA:FE-000123|CUFE:" + cufe);

        Path pdf = new InvoicePdfGenerator().generate(document, tempDir.resolve("invoices").resolve("factura_000123.pdf"));

        assertTrue(Files.size(pdf) > 1000);
        assertEquals("%PDF", new String(Files.readAllBytes(pdf), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        // Read from bytes: a file-based reader can keep the file mapped on Windows.
        PdfReader reader = new PdfReader(Files.readAllBytes(pdf));
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(text.contains("FE-000123"), "invoice number");
            assertTrue(text.contains("Carlos Pérez Gómez"), "customer");
            assertTrue(text.contains("Pensión Mensual"), "concept");
            assertTrue(text.contains("PENDIENTE DE CONFIGURACIÓN"), "unconfigured NIT is explicit");
            assertTrue(text.contains("DIAN"), "simulation notice");
        } finally {
            reader.close();
        }
        try (var files = Files.list(pdf.getParent())) {
            assertEquals(1, files.count(), "no temporary files are left");
        }
    }

    @Test
    void pdfFailureIsReportedAsIoException() throws IOException {
        Path blocker = Files.writeString(tempDir.resolve("not-a-folder"), "x");
        InvoiceDocument document = new InvoiceDocument("FE-000001", ISSUED_AT, new IssuerInfo("X", "N", "A", "T", "E", "R"),
                "C", "D", "A", "S", "R", "Efectivo", List.of(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ONE, "c", "q");
        assertThrows(IOException.class,
                () -> new InvoicePdfGenerator().generate(document, blocker.resolve("factura_000001.pdf")));
    }
}
