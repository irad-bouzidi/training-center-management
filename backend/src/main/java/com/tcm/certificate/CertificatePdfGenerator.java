package com.tcm.certificate;

import com.tcm.certificate.model.Certificate;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
 * course, the date, the certificate number and a signature line - in
 * English or in French, the wording and the date format following the
 * {@link CertificateLanguage}.
 *
 * Nothing here touches the database or the filesystem - it takes a
 * {@link Certificate} and gives back bytes, which is what makes it testable
 * on its own.
 *
 * The file is pinned to PDF 1.7. OpenPDF 3 writes 2.0 by default, and 2.0
 * no longer lets the standard fonts (Helvetica here) go unembedded - readers
 * that hold the file to that refuse to open it. 1.7 is what every viewer
 * reads, and Helvetica's WinAnsi encoding covers the French accents.
 */
@Component
public class CertificatePdfGenerator {

    public byte[] generate(Certificate certificate, CertificateLanguage language) {
        Wording wording = Wording.of(language);
        Document document = new Document(PageSize.A4.rotate(), 56, 56, 56, 56);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPdfVersion(PdfWriter.VERSION_1_7);
        document.addTitle(wording.title() + " - " + certificate.getCertificateNumber());
        document.addSubject(certificate.getCourse().getName());
        document.addAuthor(wording.centreName());
        document.open();
        drawBorder(writer);

        document.add(centered(wording.centreName().toUpperCase(language.locale()),
                FontFactory.getFont(FontFactory.HELVETICA, 12), 0, 24));
        document.add(centered(wording.title(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32), 0, 32));

        document.add(centered(wording.certifyThat(),
                FontFactory.getFont(FontFactory.HELVETICA, 13), 0, 12));
        document.add(centered(fullName(certificate),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24), 0, 12));
        document.add(centered(wording.hasCompleted(),
                FontFactory.getFont(FontFactory.HELVETICA, 13), 0, 12));
        document.add(centered(certificate.getCourse().getName() + " (" + certificate.getCourse().getCode() + ")",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18), 0, 36));

        document.add(centered(wording.issuedOn() + " " + issuedOn(certificate, language),
                FontFactory.getFont(FontFactory.HELVETICA, 12), 0, 6));
        document.add(centered(wording.number() + " " + certificate.getCertificateNumber(),
                FontFactory.getFont(FontFactory.HELVETICA, 11), 0, 48));

        document.add(centered("______________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 12), 0, 4));
        document.add(centered(wording.signature(),
                FontFactory.getFont(FontFactory.HELVETICA, 10), 0, 0));

        document.close();
        return out.toByteArray();
    }

    /** "2 March 2026" / "2 mars 2026". */
    private static String issuedOn(Certificate certificate, CertificateLanguage language) {
        return DateTimeFormatter.ofPattern("d MMMM yyyy", language.locale())
                .withZone(ZoneId.systemDefault())
                .format(certificate.getIssuedAt());
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

    /** Everything on the page that isn't the certificate's own data. */
    private record Wording(String centreName, String title, String certifyThat, String hasCompleted,
                           String issuedOn, String number, String signature) {

        static Wording of(CertificateLanguage language) {
            return switch (language) {
                case EN -> new Wording(
                        "Training Center",
                        "Certificate of Completion",
                        "This is to certify that",
                        "has successfully completed the training program",
                        "Issued on",
                        "Certificate No.",
                        "Authorised signature");
                case FR -> new Wording(
                        "Centre de formation",
                        "Certificat de réussite",
                        "Nous certifions que",
                        "a suivi avec succès le programme de formation",
                        "Délivré le",
                        "Certificat n°",
                        "Signature autorisée");
            };
        }
    }
}
