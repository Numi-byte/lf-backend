package it.bz.sta.lf;


import it.bz.sta.lf.dto.LocationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;


@RestController
@RequestMapping("/locations")
public class LocationController {
    private final LocationRepository locations;
    private final DepotRepository depots;
    public LocationController(LocationRepository locations, DepotRepository depots){ this.locations = locations; this.depots = depots; }


    record CreateLocation(UUID depotId, String zone, Integer rowNo, Integer colNo, String bin, String type) {}


    @GetMapping
    public List<LocationDto> list(@RequestParam(name = "depotId", required = false) UUID depotId) {
        return locations.findAll().stream()
                .filter(l -> depotId == null || (l.getDepot() != null && depotId.equals(l.getDepot().getId())))
                .map(LocationDto::from)
                .toList();
    }


    @PostMapping
    public ResponseEntity<LocationDto> create(@RequestBody CreateLocation req){
        Depot d = depots.findById(req.depotId()).orElseThrow();
        Location l = new Location();
        l.setId(UUID.randomUUID());
        l.setDepot(d);
        l.setZone(req.zone()); l.setRowNo(req.rowNo()); l.setColNo(req.colNo());
        l.setBin(req.bin()); l.setType(req.type());
        return ResponseEntity.status(201).body(LocationDto.from(locations.save(l)));
    }
}