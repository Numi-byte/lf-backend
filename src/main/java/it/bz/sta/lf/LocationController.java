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
    private final CompanyAccessService companyAccess;

    public LocationController(LocationRepository locations, DepotRepository depots, CompanyAccessService companyAccess) {
        this.locations = locations;
        this.depots = depots;
        this.companyAccess = companyAccess;
    }

    record CreateLocation(UUID depotId, String zone, Integer rowNo, Integer colNo, String bin, String type) {}

    @GetMapping
    public List<LocationDto> list(
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to list locations");
        String company = companyAccess.requireCompany(user);

        if (depotId != null) {
            Depot depot = depots.findById(depotId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));
            companyAccess.ensureDepotAccess(company, depot, "depot not found");
        }

        return locations.findAll().stream()
                .filter(location -> depotId == null || (location.getDepot() != null && depotId.equals(location.getDepot().getId())))
                .filter(location -> companyAccess.canAccessLocation(company, location))
                .map(LocationDto::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(
            @RequestBody CreateLocation req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to create locations");
        String company = companyAccess.requireCompany(user);

        if (req == null || req.depotId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "depotId is required");
        }

        Depot depot = depots.findById(req.depotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));
        companyAccess.ensureDepotAccess(company, depot, "depot not found");

        Location location = new Location();
        location.setId(UUID.randomUUID());
        location.setDepot(depot);
        location.setZone(req.zone());
        location.setRowNo(req.rowNo());
        location.setColNo(req.colNo());
        location.setBin(req.bin());
        location.setType(req.type());

        return ResponseEntity.status(201).body(LocationDto.from(locations.save(location)));
    }

    private static void requireUser(String user, String message) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
    }
}
