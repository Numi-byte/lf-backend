package it.bz.sta.lf.dto;

import it.bz.sta.lf.Item;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicItemDto(
        UUID id,
        String description,
        OffsetDateTime foundAt,
        String depotName,
        UUID currentLocationId,
        String categoryMain,
        String categorySub,
        String transportType,
        String transportLine,
        String transportLineDe
) {
    public static PublicItemDto fromAnonymous(Item it) {
        return new PublicItemDto(
                it.getId(),
                it.getDescription(),
                it.getFoundAt(),
                null, // IMPORTANT: hide depotName for anonymous
                it.getCurrentLocation() != null ? it.getCurrentLocation().getId() : null,
                it.getCategoryMain(),
                it.getCategorySub(),
                it.getTransportType(),
                it.getTransportLine(),
                it.getTransportLineDe()
        );
    }

    public static PublicItemDto fromMember(Item it) {
        String depotName = null;
        if (it.getCurrentLocation() != null && it.getCurrentLocation().getDepot() != null) {
            depotName = it.getCurrentLocation().getDepot().getName();
        }
        return new PublicItemDto(
                it.getId(),
                it.getDescription(),
                it.getFoundAt(),
                depotName,
                it.getCurrentLocation() != null ? it.getCurrentLocation().getId() : null,
                it.getCategoryMain(),
                it.getCategorySub(),
                it.getTransportType(),
                it.getTransportLine(),
                it.getTransportLineDe()
        );
    }
}
