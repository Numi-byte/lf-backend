package it.bz.sta.lf;


import it.bz.sta.lf.dto.DepotDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;


@RestController
@RequestMapping("/depots")
public class DepotController {
    private final DepotRepository depots;
    public DepotController(DepotRepository depots){ this.depots = depots; }


    record CreateDepot(String name) {}


    @GetMapping
    public List<DepotDto> list(){ return depots.findAll().stream().map(DepotDto::from).toList(); }


    @PostMapping
    public ResponseEntity<DepotDto> create(@RequestBody CreateDepot req){
        Depot d = new Depot(UUID.randomUUID(), req.name());
        return ResponseEntity.status(201).body(DepotDto.from(depots.save(d)));
    }
}