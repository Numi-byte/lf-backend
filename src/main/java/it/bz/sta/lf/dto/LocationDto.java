package it.bz.sta.lf.dto;


import java.util.UUID;
import it.bz.sta.lf.Location;


public record LocationDto(UUID id, UUID depotId, String zone, Integer rowNo, Integer colNo, String bin, String type) {
    public static LocationDto from(Location l){
        return new LocationDto(l.getId(), l.getDepot().getId(), l.getZone(), l.getRowNo(), l.getColNo(), l.getBin(), l.getType());
    }
}