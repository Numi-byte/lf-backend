package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByItemId(UUID itemId);

    // NEW: for "my claims"
    List<Claim> findByPassengerEmailAndPublicUserId(String passengerEmail, String publicUserId);
}
