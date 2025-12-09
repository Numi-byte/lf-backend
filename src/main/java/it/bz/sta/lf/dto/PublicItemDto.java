package it.bz.sta.lf.dto;

import it.bz.sta.lf.Item;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public-facing view of an item.
 * - No location grid
 * - No photos
 * - No claims or personal data
 */
public record PublicItemDto(
        UUID id,
        String description,
        OffsetDateTime foundAt,
        String depotName
) {
    public static PublicItemDto from(Item item) {
        String depotName = null;
        if (item.getCurrentLocation() != null && item.getCurrentLocation().getDepot() != null) {
            depotName = item.getCurrentLocation().getDepot().getName();
        }
        return new PublicItemDto(
                item.getId(),
                item.getDescription(),
                item.getFoundAt(),
                depotName
        );
    }
}
