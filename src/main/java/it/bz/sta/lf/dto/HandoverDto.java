package it.bz.sta.lf.dto;

import it.bz.sta.lf.Handover;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HandoverDto(
        UUID id,
        UUID itemId,
        UUID depotId,
        String type,
        String performedBy,
        String personName,
        String documentType,
        String documentNumber,
        String comuneName,
        String comuneReference,
        String notes,
        String attachmentKey,
        String docFrontKey,
        String docBackKey,
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
