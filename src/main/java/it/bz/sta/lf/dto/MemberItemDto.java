package it.bz.sta.lf.dto;

import it.bz.sta.lf.Item;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Logged-in public user view.
 * - Still no grid location ID
 * - Depot name is allowed
 * - Photos are fetched via /items/{id}/photos (publicly accessible)
 */
public record MemberItemDto(
        UUID id,
        String description,
        OffsetDateTime foundAt,
        String depotName,
        String categoryMain,
        String categorySub,
        String transportType,
        String transportLine,
        String transportLineDe
) {
    public static MemberItemDto from(Item it) {
        String depotName = null;
        if (it.getCurrentLocation() != null && it.getCurrentLocation().getDepot() != null) {
            depotName = it.getCurrentLocation().getDepot().getName();
        }
        return new MemberItemDto(
                it.getId(),
                it.getDescription(),
                it.getFoundAt(),
                depotName,
                it.getCategoryMain(),
                it.getCategorySub(),
                it.getTransportType(),
                it.getTransportLine(),
                it.getTransportLineDe()
        );
    }
}
