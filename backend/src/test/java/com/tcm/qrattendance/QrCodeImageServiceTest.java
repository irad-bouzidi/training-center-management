package com.tcm.qrattendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The rendering half of TCM-27, which needs nothing but a string. */
class QrCodeImageServiceTest {

    private final QrCodeImageService imageService = new QrCodeImageService();

    @Test
    void render_producesAPngThatScansBackToTheSameUrl() throws Exception {
        String url = "http://localhost:5173/attend/9f1c0b6e-0000-4000-8000-000000000001?token=abc.def";

        byte[] png = imageService.render(url);

        assertThat(png).isNotEmpty();
        assertThat(decode(png)).isEqualTo(url);
    }

    private static String decode(byte[] png) throws Exception {
        return new QRCodeReader()
                .decode(new BinaryBitmap(new HybridBinarizer(
                        new BufferedImageLuminanceSource(read(png)))))
                .getText();
    }

    private static java.awt.image.BufferedImage read(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }
}
