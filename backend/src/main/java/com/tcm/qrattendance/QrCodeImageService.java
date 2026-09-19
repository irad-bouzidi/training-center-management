package com.tcm.qrattendance;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Renders a check-in URL as a QR PNG (TCM-27). Nothing here knows about
 * sessions or tokens - it takes text and gives back an image, which is what
 * makes it testable on its own.
 */
@Service
public class QrCodeImageService {

    /** Big enough to scan from the back of a room off a projector. */
    private static final int SIZE_PX = 512;

    private static final Map<EncodeHintType, Object> HINTS = Map.of(
            // A code on a screen is read at an angle and half-glare; the
            // strongest correction level costs capacity this payload has to
            // spare.
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN, 1);

    public byte[] render(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, SIZE_PX, SIZE_PX, HINTS);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Could not encode the QR code", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the QR image", e);
        }
    }
}
