package it.bz.sta.lf.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CustomerEmailOtpRepository extends JpaRepository<CustomerEmailOtpEntity, Long> {
    long countByEmailAndCreatedAtAfter(String email, Instant createdAfter);

    Optional<CustomerEmailOtpEntity> findFirstByEmailAndDeviceHashAndConsumedAtIsNullAndExpiresAtAfterAndAttemptsRemainingGreaterThanOrderByCreatedAtDesc(
            String email,
            String deviceHash,
            Instant now,
            int attemptsRemaining
    );

    @Modifying
    @Query("delete from CustomerEmailOtpEntity o where o.expiresAt < :now or o.consumedAt is not null")
    void deleteExpiredOrConsumed(@Param("now") Instant now);
}