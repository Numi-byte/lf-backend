package it.bz.sta.lf.dto;


import java.util.UUID;
import it.bz.sta.lf.Depot;


public record DepotDto(UUID id, String name) {
    public static DepotDto from(Depot d){ return new DepotDto(d.getId(), d.getName()); }
}