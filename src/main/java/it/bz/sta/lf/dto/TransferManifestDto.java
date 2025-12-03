package it.bz.sta.lf.dto;

import it.bz.sta.lf.TransferManifest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TransferManifestDto(
        UUID id,
        UUID depotId,
        String depotName,
        String comuneName,
        String comuneContact,
        Integer boxesCount,
        String sealsCount,
        String preparedBy,
        OffsetDateTime preparedAt,
        String signedBy,
        OffsetDateTime signedAt,
        String status,
        String signatureUrl,
        List<TransferManifestItemDto> items
) {
    public static TransferManifestDto from(
            TransferManifest m,
            String signatureUrl,
            List<TransferManifestItemDto> items
    ) {
        return new TransferManifestDto(
                m.getId(),
                m.getDepot() != null ? m.getDepot().getId() : null,
                m.getDepot() != null ? m.getDepot().getName() : null,
                m.getComuneName(),
                m.getComuneContact(),
                m.getBoxesCount(),
                m.getSealsCount(),
                m.getPreparedBy(),
                m.getPreparedAt(),
                m.getSignedBy(),
                m.getSignedAt(),
                m.getStatus(),
                signatureUrl,
                items
        );
    }
}
