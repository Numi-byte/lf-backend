package it.bz.sta.lf.dto;

import it.bz.sta.lf.Item;

import java.time.OffsetDateTime;
import java.util.UUID;


public record ItemDto(
        UUID id,
        String description,
        OffsetDateTime foundAt,
        String state,
        UUID currentLocationId,
        String company,
        String categoryMain,
        String categorySub,
        String transportType,
        String transportLine,
        String transportLineDe
) {
    public static ItemDto from(Item it) {
        return new ItemDto(
                it.getId(),
                it.getDescription(),
                it.getFoundAt(),
                it.getState(),
                it.getCurrentLocation() != null ? it.getCurrentLocation().getId() : null,
                it.getCompany(),
                it.getCategoryMain(),
                it.getCategorySub(),
                it.getTransportType(),
                it.getTransportLine(),
                it.getTransportLineDe()
        );
    }
}
