package com.littlehorizons.erp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * QR codes with ZXing: generation as PNG bytes and reading back (used by the
 * tests to verify the content).
 */
public final class QrGenerator {

    public static final int DEFAULT_SIZE = 220;

    private QrGenerator() {
    }

    /** PNG image of a QR code with UTF-8 content and medium error correction. */
    public static byte[] toPng(String content, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", png);
            return png.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not generate the QR code", e);
        }
    }

    /** Decodes a QR code from PNG bytes. */
    public static String read(byte[] png) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null) {
                throw new IllegalArgumentException("The bytes are not a readable image");
            }
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints).getText();
        } catch (IOException | NotFoundException e) {
            throw new IllegalArgumentException("No QR code could be read from the image", e);
        }
    }
}
