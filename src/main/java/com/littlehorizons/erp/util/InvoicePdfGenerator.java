package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.finance.InvoiceDocument;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

/**
 * Arma el PDF de la factura con OpenPDF.
 *
 * <p>Primero se escribe en un archivo temporal en la misma carpeta y luego
 * se mueve al nombre final, así una generación interrumpida nunca deja un
 * {@code factura_NNNNNN.pdf} a medio escribir.</p>
 */
public class InvoicePdfGenerator {

    public static final String SIMULATION_NOTICE = "DOCUMENTO DE DEMOSTRACIÓN ACADÉMICA. El CUFE, el código QR y la "
            + "resolución de facturación son simulados: este documento NO es una factura electrónica validada ni "
            + "transmitida a la DIAN y no tiene validez fiscal.";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Color HEADER_COLOR = new Color(31, 78, 121);
    private static final Color LIGHT_GRAY = new Color(235, 239, 243);
    private static final Font TITLE = new Font(Font.HELVETICA, 15, Font.BOLD, HEADER_COLOR);
    private static final Font BOLD = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font SMALL = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
    private static final Font WHITE_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font WARNING = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(170, 40, 40));

    /** Escribe el PDF y devuelve su ruta absoluta. */
    public Path generate(InvoiceDocument invoice, Path targetFile) throws IOException {
        Path target = targetFile.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "factura-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                write(invoice, output);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Algunas carpetas sincronizadas (OneDrive) no permiten movimientos atómicos; se usa un movimiento normal como alternativa.
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Arma el documento PDF completo: encabezado, cliente, detalle, totales, CUFE/QR y aviso legal. */
    private void write(InvoiceDocument invoice, OutputStream output) throws IOException {
        Document document = new Document(PageSize.LETTER, 40, 40, 36, 36);
        try {
            PdfWriter.getInstance(document, output);
            document.addTitle("Factura " + invoice.number());
            document.addAuthor(invoice.issuer().businessName());
            document.open();
            document.add(header(invoice));
            document.add(spacer());
            document.add(customer(invoice));
            document.add(spacer());
            document.add(details(invoice));
            document.add(totals(invoice));
            document.add(spacer());
            document.add(validation(invoice));
            Paragraph notice = new Paragraph(SIMULATION_NOTICE, WARNING);
            notice.setSpacingBefore(10);
            document.add(notice);
        } catch (DocumentException e) {
            throw new IOException("Could not build the invoice PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /** Encabezado de la factura: datos del colegio a la izquierda, número y fechas a la derecha. */
    private static PdfPTable header(InvoiceDocument invoice) {
        PdfPTable table = new PdfPTable(new float[] {3, 2});
        table.setWidthPercentage(100);
        PdfPCell issuer = new PdfPCell();
        issuer.setBorder(Rectangle.NO_BORDER);
        issuer.addElement(new Paragraph(invoice.issuer().businessName(), TITLE));
        issuer.addElement(new Paragraph("NIT: " + invoice.issuer().nit(), BOLD));
        issuer.addElement(new Paragraph("Dirección: " + invoice.issuer().address(), NORMAL));
        issuer.addElement(new Paragraph("Teléfono: " + invoice.issuer().phone(), NORMAL));
        issuer.addElement(new Paragraph("Correo: " + invoice.issuer().email(), NORMAL));
        table.addCell(issuer);

        PdfPCell data = new PdfPCell();
        data.setBackgroundColor(LIGHT_GRAY);
        data.setPadding(8);
        data.setBorderColor(HEADER_COLOR);
        data.addElement(new Paragraph("FACTURA DE VENTA", BOLD));
        data.addElement(new Paragraph("(representación gráfica simulada)", SMALL));
        data.addElement(new Paragraph("No. " + invoice.number(), new Font(Font.HELVETICA, 13, Font.BOLD)));
        data.addElement(new Paragraph("Fecha y hora de emisión: " + DATE_TIME.format(invoice.issuedAt()), NORMAL));
        data.addElement(new Paragraph("Recibo de pago: " + invoice.receiptNumber(), NORMAL));
        data.addElement(new Paragraph("Medio de pago: " + invoice.paymentMethod(), NORMAL));
        table.addCell(data);
        return table;
    }

    /** Tabla con los datos del cliente (acudiente responsable) y del estudiante. */
    private static PdfPTable customer(InvoiceDocument invoice) {
        PdfPTable table = new PdfPTable(new float[] {1, 3});
        table.setWidthPercentage(100);
        PdfPCell title = new PdfPCell(new Phrase("DATOS DEL CLIENTE (ACUDIENTE RESPONSABLE)", WHITE_BOLD));
        title.setColspan(2);
        title.setBackgroundColor(HEADER_COLOR);
        title.setPadding(5);
        table.addCell(title);
        addRow(table, "Nombre", invoice.customerName());
        addRow(table, "Documento", invoice.customerDocument());
        addRow(table, "Dirección", invoice.customerAddress());
        addRow(table, "Estudiante", invoice.studentName());
        return table;
    }

    /** Tabla de detalle: concepto, cantidad, valor unitario y subtotal. */
    private static PdfPTable details(InvoiceDocument invoice) {
        PdfPTable table = new PdfPTable(new float[] {5, 1, 2, 2});
        table.setWidthPercentage(100);
        for (String heading : new String[] {"Descripción del concepto", "Cantidad", "Valor unitario", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(heading, WHITE_BOLD));
            cell.setBackgroundColor(HEADER_COLOR);
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (InvoiceDocument.Line line : invoice.lines()) {
            table.addCell(cell(line.description(), Element.ALIGN_LEFT));
            table.addCell(cell(String.valueOf(line.quantity()), Element.ALIGN_CENTER));
            table.addCell(cell(Formats.money(line.unitValue()), Element.ALIGN_RIGHT));
            table.addCell(cell(Formats.money(line.subtotal()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    /** Tabla de totales: subtotal, IVA (o "no aplica") y total. */
    private static PdfPTable totals(InvoiceDocument invoice) {
        PdfPTable table = new PdfPTable(new float[] {8, 2});
        table.setWidthPercentage(100);
        addTotal(table, "Subtotal", invoice.subtotal(), NORMAL);
        String taxLabel = invoice.tax().signum() == 0
                ? "IVA (no aplica)"
                : "IVA (" + invoice.taxRatePercent().stripTrailingZeros().toPlainString() + " %)";
        addTotal(table, taxLabel, invoice.tax(), NORMAL);
        addTotal(table, "TOTAL A PAGAR", invoice.total(), BOLD);
        return table;
    }

    /** Bloque de validación: CUFE simulado y código QR, con el aviso de que es una simulación. */
    private static PdfPTable validation(InvoiceDocument invoice) throws IOException {
        PdfPTable table = new PdfPTable(new float[] {4, 1.6f});
        table.setWidthPercentage(100);
        PdfPCell text = new PdfPCell();
        text.setBorder(Rectangle.NO_BORDER);
        text.addElement(new Paragraph("CUFE (simulado, SHA-384):", BOLD));
        text.addElement(new Paragraph(invoice.cufe(), SMALL));
        Paragraph resolution = new Paragraph();
        resolution.add(new Chunk("Resolución de habilitación: ", BOLD));
        resolution.add(new Chunk(invoice.issuer().resolutionText(), NORMAL));
        resolution.setSpacingBefore(6);
        text.addElement(resolution);
        text.addElement(new Paragraph("El código QR contiene el número de factura, la fecha, el total, el NIT del "
                + "emisor y el CUFE; no incluye datos personales del cliente.", SMALL));
        table.addCell(text);

        Image qr;
        try {
            qr = Image.getInstance(QrGenerator.toPng(invoice.qrContent(), QrGenerator.DEFAULT_SIZE));
        } catch (DocumentException e) {
            throw new IOException("Could not embed the QR image", e);
        }
        qr.scaleToFit(110, 110);
        PdfPCell qrCell = new PdfPCell(qr, false);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(qrCell);
        return table;
    }

    /** Agrega una fila etiqueta-valor a una tabla (con "-" si el valor está vacío). */
    private static void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD));
        labelCell.setBackgroundColor(LIGHT_GRAY);
        labelCell.setPadding(4);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, NORMAL));
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    /** Agrega una fila de total alineada a la derecha. */
    private static void addTotal(PdfPTable table, String label, BigDecimal value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(Formats.money(value), font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    /** Celda de tabla con texto y alineación. */
    private static PdfPCell cell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }

    /** Párrafo vacío pequeño que sirve como espacio entre bloques. */
    private static Paragraph spacer() {
        Paragraph paragraph = new Paragraph(" ", SMALL);
        paragraph.setSpacingAfter(4);
        return paragraph;
    }
}
