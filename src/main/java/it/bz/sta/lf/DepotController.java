package it.bz.sta.lf;

import it.bz.sta.lf.dto.DepotDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/depots")
public class DepotController {

    private final DepotRepository depots;
    private final CompanyAccessService companyAccess;

    public DepotController(DepotRepository depots, CompanyAccessService companyAccess) {
        this.depots = depots;
        this.companyAccess = companyAccess;
    }

    record CreateDepot(String name) {}

    @GetMapping
    public List<DepotDto> list(
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to list depots");
        String company = companyAccess.requireCompany(user);

        return depots.findAll().stream()
                .filter(depot -> companyAccess.canAccessDepot(company, depot))
                .map(DepotDto::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DepotDto> create(
            @RequestBody CreateDepot req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to create depots");

        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }

        Depot depot = new Depot();
        depot.setId(UUID.randomUUID());
        depot.setName(req.name().trim());
        companyAccess.assignDepotCompany(depot, user);

        return ResponseEntity.status(201).body(DepotDto.from(depots.save(depot)));
    }

    private static void requireUser(String user, String message) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
    }
}
