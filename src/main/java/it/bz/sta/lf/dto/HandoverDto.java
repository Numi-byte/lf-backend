package it.bz.sta.lf.dto;

import it.bz.sta.lf.Handover;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HandoverDto(
        UUID id,
        UUID itemId,
        UUID depotId,

        // PERSON | COMUNE
        String type,

        String performedBy,

        // PERSON handover
        String personName,
        String documentType,
        String documentNumber,

        // COMUNE handover
        String comuneName,
        String comuneReference,

        String notes,
        String attachmentKey,

        // raw keys in S3/MinIO (for debugging / admin)
        String docFrontKey,
        String docBackKey,

        // presigned URLs (for React UI thumbnails / full view)
        String docFrontUrl,
        String docBackUrl,

        OffsetDateTime createdAt
) {
    public static HandoverDto from(Handover h, String docFrontUrl, String docBackUrl) {
        return new HandoverDto(
                h.getId(),
                h.getItem() != null ? h.getItem().getId() : null,
                h.getDepotId(),
                h.getType(),
                h.getPerformedBy(),
                h.getPersonName(),
                h.getDocumentType(),
                h.getDocumentNumber(),
                h.getComuneName(),
                h.getComuneReference(),
                h.getNotes(),
                h.getAttachmentKey(),
                h.getDocFrontKey(),
                h.getDocBackKey(),
                docFrontUrl,
                docBackUrl,
                h.getCreatedAt()
        );
    }
}
