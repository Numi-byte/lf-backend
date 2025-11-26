package it.bz.sta.lf;

import it.bz.sta.lf.dto.ClaimDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimRepository claims;
    private final ItemRepository items;
    private final AuditService audits;

    public ClaimController(ClaimRepository claims, ItemRepository items, AuditService audits) {
        this.claims = claims;
        this.items = items;
        this.audits = audits;
    }

    // ----- Request DTOs -----
    public record CreateClaim(
            UUID itemId,
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            String narrative
    ) {}

    public record ApproveReq(
            String method,
            Integer feeCents
    ) {}

    // ----- List & get -----
    @GetMapping
    public List<ClaimDto> list(@RequestParam(name = "itemId", required = false) UUID itemId) {
        List<Claim> src = (itemId == null) ? claims.findAll() : claims.findByItemId(itemId);
        return src.stream().map(ClaimDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDto> get(@PathVariable("id") UUID id) {
        return claims.findById(id)
                .map(c -> ResponseEntity.ok(ClaimDto.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ----- Create claim -----
    @PostMapping
    public ResponseEntity<ClaimDto> create(@RequestBody CreateClaim req) {
        if (req == null || req.itemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required");
        }

        Item item = items.findById(req.itemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        Claim c = new Claim();
        c.setId(UUID.randomUUID());
        c.setItem(item);
        c.setPassengerName(req.passengerName());
        c.setPassengerEmail(req.passengerEmail());
        c.setPassengerPhone(req.passengerPhone());
        c.setNarrative(req.narrative());
        c.setStatus("new");
        c.setSubmittedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());

        Claim saved = claims.save(c);

        audits.log(
                "CLAIM_CREATED",
                "CLAIM",
                saved.getId(),
                null,
                "{\"itemId\":\"" + item.getId() + "\"}"
        );

        return ResponseEntity.status(201).body(ClaimDto.from(saved));
    }

    // ----- Approve claim → put item ON_HOLD -----
    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ClaimDto> approve(
            @PathVariable("id") UUID id,
            @RequestBody ApproveReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        Claim c = claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));

        if (req == null || req.method() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "method is required");
        }

        c.setStatus("approved");
        c.setMethod(req.method());
        c.setFeeCents(req.feeCents() == null ? 0 : req.feeCents());
        c.setUpdatedAt(OffsetDateTime.now());

        // 🔹 Link to item → set ON_HOLD
        Item item = c.getItem();
        if (item != null) {
            String before = "{\"state\":\"" + item.getState() + "\"}";
            item.setState(Item.STATE_ON_HOLD);
            String after = "{\"state\":\"" + item.getState() + "\"}";

            audits.log(
                    "CLAIM_APPROVED",
                    "CLAIM",
                    c.getId(),
                    user,
                    "{\"itemId\":\"" + item.getId() + "\",\"before\":" + before + ",\"after\":" + after + "}"
            );
        } else {
            audits.log("CLAIM_APPROVED", "CLAIM", c.getId(), user, null);
        }

        return ResponseEntity.ok(ClaimDto.from(c));
    }

    // ----- Close claim (manual) -----
    @PostMapping("/{id}/close")
    @Transactional
    public ResponseEntity<ClaimDto> close(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        Claim c = claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));

        c.setStatus("closed");
        c.setUpdatedAt(OffsetDateTime.now());

        audits.log("CLAIM_CLOSED", "CLAIM", c.getId(), user, null);

        return ResponseEntity.ok(ClaimDto.from(c));
    }
}
