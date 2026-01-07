package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByItemId(UUID itemId);

    // NEW: My-claims (ordered newest first)
    List<Claim> findByPublicUserIdAndPassengerEmailOrderByUpdatedAtDesc(String publicUserId, String passengerEmail);

    // OPTIONAL: CS / support can find claim by reference code later
    List<Claim> findByPublicReferenceCode(String publicReferenceCode);
}
