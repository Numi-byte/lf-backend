package it.bz.sta.lf;

import it.bz.sta.lf.dto.ClaimDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/public/claims")
public class PublicClaimController {

    private final ClaimRepository claims;

    public PublicClaimController(ClaimRepository claims) {
        this.claims = claims;
    }

    /**
     * "My claims" for public users.
     *
     * Filters by:
     *  - X-User      (public_user_id)
     *  - passengerEmail (query param)
     *
     * GET /public/claims?email=mario.rossi@example.com
     */
    @GetMapping
    public List<ClaimDto> myClaims(
            @RequestParam("email") String email,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to view claims");
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }

        List<Claim> list = claims.findByPassengerEmailAndPublicUserId(email, user);
        return list.stream().map(ClaimDto::from).toList();
    }
}
