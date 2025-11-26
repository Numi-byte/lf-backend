package it.bz.sta.lf.dto;


import it.bz.sta.lf.Claim;
import java.time.OffsetDateTime;
import java.util.UUID;


public record ClaimDto(
        UUID id,
        UUID itemId,
        String status,
        String method,
        Integer feeCents,
        String currency,
        String passengerName,
        String passengerEmail,
        String passengerPhone,
        String narrative,
        OffsetDateTime submittedAt,
        OffsetDateTime updatedAt
) {
    public static ClaimDto from(Claim c){
        return new ClaimDto(
                c.getId(),
                c.getItem().getId(),
                c.getStatus(),
                c.getMethod(),
                c.getFeeCents(),
                c.getCurrency(),
                c.getPassengerName(),
                c.getPassengerEmail(),
                c.getPassengerPhone(),
                c.getNarrative(),
                c.getSubmittedAt(),
                c.getUpdatedAt()
        );
    }
}