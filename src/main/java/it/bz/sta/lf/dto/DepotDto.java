package it.bz.sta.lf.dto;


import it.bz.sta.lf.Depot;
import java.util.UUID;


public record DepotDto(UUID id, String name, String company) {
    public static DepotDto from(Depot d) {
        return new DepotDto(d.getId(), d.getName(), d.getCompany());
    }
}