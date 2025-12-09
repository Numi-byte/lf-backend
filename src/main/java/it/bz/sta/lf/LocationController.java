package it.bz.sta.lf;

import it.bz.sta.lf.dto.LocationDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationRepository locations;
    private final DepotRepository depots;

    public LocationController(LocationRepository locations, DepotRepository depots) {
        this.locations = locations;
        this.depots = depots;
    }

    record CreateLocation(UUID depotId, String zone, Integer rowNo, Integer colNo, String bin, String type) {}

    @GetMapping
    public List<LocationDto> list(
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to list locations");
        }

        return locations.findAll().stream()
                .filter(l -> depotId == null || (l.getDepot() != null && depotId.equals(l.getDepot().getId())))
                .map(LocationDto::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(
            @RequestBody CreateLocation req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to create locations");
        }

        if (req == null || req.depotId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "depotId is required");
        }

        Depot d = depots.findById(req.depotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));

        Location l = new Location();
        l.setId(UUID.randomUUID());
        l.setDepot(d);
        l.setZone(req.zone());
        l.setRowNo(req.rowNo());
        l.setColNo(req.colNo());
        l.setBin(req.bin());
        l.setType(req.type());

        return ResponseEntity.status(201).body(LocationDto.from(locations.save(l)));
    }
}
