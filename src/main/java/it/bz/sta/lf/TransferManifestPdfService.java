package it.bz.sta.lf;

import it.bz.sta.lf.storage.S3StorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransferManifestPdfService {

    private static final String TEMPLATE_PATH = "templates/STA_Template (1).pdf";
    private static final float TEMPLATE_HEADER_RESERVED_HEIGHT = 120f;
    private static final float MARGIN = 48f;
    private static final float LINE_HEIGHT = 16f;
    private final TransferManifestPdfRepository pdfRepository;
    private final S3StorageService storage;

    public TransferManifestPdfService(TransferManifestPdfRepository pdfRepository, S3StorageService storage) {
        this.pdfRepository = pdfRepository;
        this.storage = storage;
    }

    @Transactional
    public byte[] getOrCreateOfficialManifestPdf(TransferManifest manifest, String lang, String generatedBy) throws IOException {
        String normalizedLang = normalizeLang(lang);

        var existing = pdfRepository.findByManifest_IdAndLang(manifest.getId(), normalizedLang);
        if (existing.isPresent()) {
            TransferManifestPdf archive = existing.get();
            if (!isArchivedPdfStale(manifest, archive)) {
                return archive.getPdfData();
            }

            byte[] pdfBytes = generateOfficialManifestPdf(manifest, normalizedLang);
            archive.setPdfData(pdfBytes);
            archive.setGeneratedAt(OffsetDateTime.now());
            archive.setGeneratedBy(generatedBy);
            pdfRepository.save(archive);
            return pdfBytes;
        }

        byte[] pdfBytes = generateOfficialManifestPdf(manifest, normalizedLang);

        TransferManifestPdf archive = new TransferManifestPdf();
        archive.setId(java.util.UUID.randomUUID());
        archive.setManifest(manifest);
        archive.setLang(normalizedLang);
        archive.setPdfData(pdfBytes);
        archive.setGeneratedAt(OffsetDateTime.now());
        archive.setGeneratedBy(generatedBy);

        try {
            pdfRepository.save(archive);
            return pdfBytes;
        } catch (DataIntegrityViolationException e) {
            return pdfRepository.findByManifest_IdAndLang(manifest.getId(), normalizedLang)
                    .map(TransferManifestPdf::getPdfData)
                    .orElseThrow(() -> e);
        }
    }

    private static boolean isArchivedPdfStale(TransferManifest manifest, TransferManifestPdf archive) {
        OffsetDateTime signedAt = manifest.getSignedAt();
        OffsetDateTime generatedAt = archive.getGeneratedAt();
        return signedAt != null && (generatedAt == null || generatedAt.isBefore(signedAt));
    }

    @Transactional
    public void deleteArchivedManifestPdfs(UUID manifestId) {
        pdfRepository.deleteByManifest_Id(manifestId);
    }

    public byte[] generateOfficialManifestPdf(TransferManifest manifest, String lang) throws IOException {
        Locale locale = toLocale(lang);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale);

        try (PDDocument document = loadTemplateDocument()) {
            PDPage page = document.getPage(0);

            try (PDPageContentStream content = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                float pageWidth = page.getMediaBox().getWidth();
                float y = page.getMediaBox().getHeight() - MARGIN - TEMPLATE_HEADER_RESERVED_HEIGHT;

                y = drawTitle(content, pageWidth, y, t("officialTitle", locale));
                y = drawLine(content, MARGIN, y, pageWidth - MARGIN, y);
                y -= 20;

                y = writeText(content, MARGIN, y, t("manifestId", locale) + ": " + manifest.getId(), PDType1Font.HELVETICA_BOLD, 11);
                y = writeText(content, MARGIN, y, t("status", locale) + ": " + nullSafe(manifest.getStatus()), PDType1Font.HELVETICA, 11);
                y = writeText(content, MARGIN, y, t("preparedBy", locale) + ": " + nullSafe(manifest.getPreparedBy()), PDType1Font.HELVETICA, 11);
                y = writeText(content, MARGIN, y, t("preparedAt", locale) + ": " + formatDate(manifest.getPreparedAt(), dateTimeFormatter), PDType1Font.HELVETICA, 11);
                y = writeText(content, MARGIN, y, t("municipality", locale) + ": " + nullSafe(manifest.getComuneName()), PDType1Font.HELVETICA, 11);
                y = writeText(content, MARGIN, y, t("municipalityContact", locale) + ": " + nullSafe(manifest.getComuneContact()), PDType1Font.HELVETICA, 11);
                y = writeText(content, MARGIN, y, t("boxes", locale) + ": " + nullSafe(manifest.getBoxesCount()), PDType1Font.HELVETICA, 11);
                y = writeText(content, MARGIN, y, t("seals", locale) + ": " + nullSafe(manifest.getSealsCount()), PDType1Font.HELVETICA, 11);
                y -= 8;

                y = writeText(content, MARGIN, y, t("itemsTitle", locale), PDType1Font.HELVETICA_BOLD, 12);
                y = writeText(content, MARGIN, y, t("itemsHeader", locale), PDType1Font.HELVETICA_BOLD, 10);

                List<TransferManifestItem> lines = manifest.getItems() == null ? List.of() : manifest.getItems();
                for (int i = 0; i < lines.size(); i++) {
                    TransferManifestItem item = lines.get(i);
                    String row = String.format(
                            "%d. %s | %s/%s | %s | %s",
                            i + 1,
                            nullSafe(item.getShortCode()),
                            nullSafe(item.getCategoryMain()),
                            nullSafe(item.getCategorySub()),
                            formatDate(item.getFoundAt(), dateTimeFormatter),
                            nullSafe(item.getFoundPlace())
                    );
                    y = writeText(content, MARGIN, y, row, PDType1Font.HELVETICA, 10);

                    if (y < 170) {
                        break;
                    }
                }

                y -= 16;
                y = writeText(content, MARGIN, y, t("declarationTitle", locale), PDType1Font.HELVETICA_BOLD, 11);
                y = writeWrapped(content, MARGIN, y, pageWidth - (2 * MARGIN), t("declarationText", locale), 10);

                y -= 12;

                float boxHeight = 112f;
                float boxWidth = pageWidth - (2 * MARGIN);
                float boxBottom = Math.max(56f, y - boxHeight);
                content.addRect(MARGIN, boxBottom, boxWidth, boxHeight);
                content.stroke();

                byte[] signatureImageBytes = loadSignatureImageBytes(manifest);
                if (signatureImageBytes != null) {
                    PDImageXObject signatureImage = PDImageXObject.createFromByteArray(document, signatureImageBytes, "manifest-signature");
                    drawImageInBox(content, signatureImage, MARGIN + 8, boxBottom + 42, boxWidth - 16, 38);
                }

                writeStatic(content, MARGIN + 8, boxBottom + boxHeight - 20, t("signaturePanel", locale), PDType1Font.HELVETICA_BOLD, 11);
                writeStatic(content, MARGIN + 8, boxBottom + boxHeight - 38, t("signatureHint", locale), PDType1Font.HELVETICA, 10);
                writeStatic(content, MARGIN + 8, boxBottom + 28, t("signatureBy", locale) + ": " + nullSafe(manifest.getSignedBy()), PDType1Font.HELVETICA, 10);
                writeStatic(content, MARGIN + 8, boxBottom + 16, t("signatureDate", locale) + ": " + formatDate(manifest.getSignedAt(), dateTimeFormatter), PDType1Font.HELVETICA, 10);
                writeStatic(content, MARGIN + 8, boxBottom + 4, t("verificationFooter", locale), PDType1Font.HELVETICA_OBLIQUE, 8);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }



    private static PDDocument loadTemplateDocument() throws IOException {
        ClassPathResource templateResource = new ClassPathResource(TEMPLATE_PATH);
        if (!templateResource.exists()) {
            PDDocument fallbackDocument = new PDDocument();
            fallbackDocument.addPage(new PDPage(PDRectangle.A4));
            return fallbackDocument;
        }

        try (InputStream templateInputStream = templateResource.getInputStream()) {
            PDDocument templateDocument = PDDocument.load(templateInputStream);
            if (templateDocument.getNumberOfPages() == 0) {
                templateDocument.addPage(new PDPage(PDRectangle.A4));
            }
            return templateDocument;
        }
    }


    private byte[] loadSignatureImageBytes(TransferManifest manifest) throws IOException {
        String signatureKey = manifest.getSignatureKey();
        if (signatureKey == null || signatureKey.isBlank()) {
            return null;
        }

        try (InputStream signatureInputStream = storage.get(signatureKey)) {
            return signatureInputStream.readAllBytes();
        } catch (Exception e) {
            throw new IOException("could not load signature image", e);
        }
    }

    private static void drawImageInBox(PDPageContentStream content, PDImageXObject image, float x, float y, float width, float height) throws IOException {
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        float scale = Math.min(width / imageWidth, height / imageHeight);
        float drawWidth = imageWidth * scale;
        float drawHeight = imageHeight * scale;
        float drawX = x + ((width - drawWidth) / 2f);
        float drawY = y + ((height - drawHeight) / 2f);
        content.drawImage(image, drawX, drawY, drawWidth, drawHeight);
    }

    private static String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String formatDate(OffsetDateTime value, DateTimeFormatter formatter) {
        return value == null ? "-" : value.format(formatter);
    }

    public static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "de";
        }
        return switch (lang.toLowerCase(Locale.ROOT)) {
            case "it", "it-it", "ita" -> "it";
            case "de", "de-de", "ger", "deu" -> "de";
            default -> "de";
        };
    }

    private static Locale toLocale(String lang) {
        String normalizedLang = normalizeLang(lang);
        return "it".equals(normalizedLang) ? Locale.ITALIAN : Locale.GERMAN;
    }

    private static float drawTitle(PDPageContentStream content, float pageWidth, float y, String text) throws IOException {
        float fontSize = 17f;
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * fontSize;
        float x = (pageWidth - textWidth) / 2f;
        writeStatic(content, x, y, text, PDType1Font.HELVETICA_BOLD, fontSize);
        return y - 24;
    }

    private static float drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
        return y1;
    }

    private static float writeText(PDPageContentStream content, float x, float y, String text, PDFont font, float size) throws IOException {
        writeStatic(content, x, y, text, font, size);
        return y - LINE_HEIGHT;
    }

    private static float writeWrapped(PDPageContentStream content, float x, float y, float width, String text, float size) throws IOException {
        var words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float candidateWidth = PDType1Font.HELVETICA.getStringWidth(candidate) / 1000 * size;
            if (candidateWidth > width && !line.isEmpty()) {
                writeStatic(content, x, y, line.toString(), PDType1Font.HELVETICA, size);
                y -= LINE_HEIGHT;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty()) {
            writeStatic(content, x, y, line.toString(), PDType1Font.HELVETICA, size);
            y -= LINE_HEIGHT;
        }
        return y;
    }

    private static void writeStatic(PDPageContentStream content, float x, float y, String text, PDFont font, float size) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private static String t(String key, Locale locale) {
        boolean it = Locale.ITALIAN.getLanguage().equals(locale.getLanguage());
        return switch (key) {
            case "officialTitle" -> it ? "VERBALE UFFICIALE DI CONSEGNA AL COMUNE" : "OFFIZIELLES ÜBERGABEPROTOKOLL AN DIE GEMEINDE";
            case "manifestId" -> it ? "ID verbale" : "Protokoll-ID";
            case "status" -> it ? "Stato" : "Status";
            case "preparedBy" -> it ? "Preparato da" : "Erstellt durch";
            case "preparedAt" -> it ? "Data preparazione" : "Erstellt am";
            case "municipality" -> it ? "Comune" : "Gemeinde";
            case "municipalityContact" -> it ? "Contatto del comune" : "Kontaktperson Gemeinde";
            case "boxes" -> it ? "Numero colli/scatole" : "Anzahl Pakete/Boxen";
            case "seals" -> it ? "Sigilli" : "Siegel";
            case "itemsTitle" -> it ? "Elenco oggetti consegnati" : "Liste der übergebenen Gegenstände";
            case "itemsHeader" -> it ? "N. | Codice | Categoria | Data ritrovamento | Luogo ritrovamento" : "Nr. | Code | Kategorie | Funddatum | Fundort";
            case "declarationTitle" -> it ? "Dichiarazione ufficiale" : "Offizielle Erklärung";
            case "declarationText" -> it
                    ? "Con il presente verbale si conferma che i beni sopra elencati vengono consegnati al Comune competente. La firma digitale apposta nel riquadro sottostante conferma la presa in carico ufficiale."
                    : "Mit diesem Protokoll wird bestätigt, dass die oben angeführten Gegenstände der zuständigen Gemeinde übergeben werden. Die im Feld unten angebrachte digitale Signatur bestätigt die offizielle Übernahme.";
            case "signaturePanel" -> it ? "Riquadro firma digitale" : "Feld für digitale Signatur";
            case "signatureHint" -> it ? "Firmare qui direttamente (tablet, touch o mouse)" : "Hier direkt unterschreiben (Tablet, Touch oder Maus)";
            case "signatureBy" -> it ? "Firmato da" : "Unterschrieben von";
            case "signatureDate" -> it ? "Data e ora firma" : "Datum und Uhrzeit";
            case "verificationFooter" -> it ? "Documento ufficiale. Conservare con ID verbale e tracciamento audit." : "Offizielles Dokument. Mit Protokoll-ID und Audit-Trail archivieren.";
            default -> key;
        };
    }
}