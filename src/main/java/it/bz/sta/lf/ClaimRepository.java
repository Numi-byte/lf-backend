package it.bz.sta.lf;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.util.UUID;


public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByItemId(UUID itemId);
}