package com.tcm.certificate;

import com.tcm.certificate.model.Certificate;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * Draws the certificate PDF, per docs/tasks/TCM-25 step 3: a landscape
 * letterhead-style page with the centre's name, the title, the student, the
 * course, the date, the certificate number and a signature line.
 *
 * Nothing here touches the database or the filesystem - it takes a
 * {@link Certificate} and gives back bytes, which is what makes it testable
 * on its own.
 */
@Component
public class CertificatePdfGenerator {

    private static final DateTimeFormatter ISSUED_ON =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private static final String CENTRE_NAME = "Training Center";

    public byte[] generate(Certificate certificate) {
        Document document = new Document(PageSize.A4.rotate(), 56, 56, 56, 56);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        drawBorder(writer);

        document.add(centered(CENTRE_NAME.toUpperCase(Locale.ENGLISH),
                FontFactory.getFont(FontFactory.HELVETICA, 12), 0, 24));
        document.add(centered("Certificate of Completion",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32), 0, 32));

        document.add(centered("This is to certify that",
                FontFactory.getFont(FontFactory.HELVETICA, 13), 0, 12));
        document.add(centered(fullName(certificate),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24), 0, 12));
        document.add(centered("has successfully completed the training program",
                FontFactory.getFont(FontFactory.HELVETICA, 13), 0, 12));
        document.add(centered(certificate.getCourse().getName() + " (" + certificate.getCourse().getCode() + ")",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18), 0, 36));

        document.add(centered("Issued on " + ISSUED_ON.format(certificate.getIssuedAt()),
                FontFactory.getFont(FontFactory.HELVETICA, 12), 0, 6));
        document.add(centered("Certificate No. " + certificate.getCertificateNumber(),
                FontFactory.getFont(FontFactory.HELVETICA, 11), 0, 48));

        document.add(centered("______________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 12), 0, 4));
        document.add(centered("Authorised signature",
                FontFactory.getFont(FontFactory.HELVETICA, 10), 0, 0));

        document.close();
        return out.toByteArray();
    }

    /** A thin inset frame - the whole of the "letterhead" this needs. */
    private static void drawBorder(PdfWriter writer) {
        Rectangle page = writer.getPageSize();
        PdfContentByte canvas = writer.getDirectContent();
        canvas.setLineWidth(1.5f);
        canvas.rectangle(28, 28, page.getWidth() - 56, page.getHeight() - 56);
        canvas.stroke();
    }

    private static Paragraph centered(String text, Font font, float spacingBefore, float spacingAfter) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private static String fullName(Certificate certificate) {
        return certificate.getStudent().getFirstName() + " " + certificate.getStudent().getLastName();
    }
}
