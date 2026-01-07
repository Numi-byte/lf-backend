package it.bz.sta.lf.dto;

import it.bz.sta.lf.TransferManifestItem;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransferManifestItemDto(
        UUID id,
        UUID itemId,
        String shortCode,
        String categoryMain,
        String categorySub,
        OffsetDateTime foundAt,
        String foundPlace,
        String photoUrl
) {
    public static TransferManifestItemDto from(TransferManifestItem line, String photoUrl) {
        return new TransferManifestItemDto(
                line.getId(),
                line.getItem().getId(),
                line.getShortCode(),
                line.getCategoryMain(),
                line.getCategorySub(),
                line.getFoundAt(),
                line.getFoundPlace(),
                photoUrl
        );
    }
}
