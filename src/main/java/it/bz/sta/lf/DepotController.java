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

    public DepotController(DepotRepository depots) {
        this.depots = depots;
    }

    record CreateDepot(String name) {}

    @GetMapping
    public List<DepotDto> list(
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to list depots");
        }

        return depots.findAll().stream()
                .map(DepotDto::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DepotDto> create(
            @RequestBody CreateDepot req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to create depots");
        }

        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }

        Depot d = new Depot(UUID.randomUUID(), req.name());
        return ResponseEntity.status(201).body(DepotDto.from(depots.save(d)));
    }
}
