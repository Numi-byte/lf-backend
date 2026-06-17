package it.bz.sta.lf;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class ClaimEmailTextService {

    private static final DateTimeFormatter SUBMITTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm XXX");

    private final ClaimEmailManagementService emailManagementService;

    public ClaimEmailTextService(ClaimEmailManagementService emailManagementService) {
        this.emailManagementService = emailManagementService;
    }

    public ClaimEmailText claimantSubmitted(Claim claim, String company) {
        ClaimEmailSetting setting = emailManagementService.getSetting(company);
        Map<String, String> placeholders = placeholders(claim, company, null, null);
        return new ClaimEmailText(
                render(setting.getClaimantSubjectTemplate(), placeholders),
                render(setting.getClaimantBodyTemplate(), placeholders)
        );
    }

    public ClaimEmailText companyReceived(Claim claim, String company) {
        ClaimEmailSetting setting = emailManagementService.getSetting(company);
        Map<String, String> placeholders = placeholders(claim, company, null, null);
        return new ClaimEmailText(
                render(setting.getCompanySubjectTemplate(), placeholders),
                render(setting.getCompanyBodyTemplate(), placeholders)
        );
    }

    public ClaimEmailText claimantUpdated(Claim claim, String company, String previousStatus, String previousItemState) {
        ClaimEmailSetting setting = emailManagementService.getSetting(company);
        Map<String, String> placeholders = placeholders(claim, company, previousStatus, previousItemState);
        return new ClaimEmailText(
                render(setting.getClaimantUpdateSubjectTemplate(), placeholders),
                render(setting.getClaimantUpdateBodyTemplate(), placeholders)
        );
    }

    public ClaimEmailText companyUpdated(Claim claim, String company, String previousStatus, String previousItemState) {
        ClaimEmailSetting setting = emailManagementService.getSetting(company);
        Map<String, String> placeholders = placeholders(claim, company, previousStatus, previousItemState);
        return new ClaimEmailText(
                render(setting.getCompanyUpdateSubjectTemplate(), placeholders),
                render(setting.getCompanyUpdateBodyTemplate(), placeholders)
        );
    }

    private Map<String, String> placeholders(Claim claim, String company, String previousStatus, String previousItemState) {
        Item item = claim.getItem();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("claimReference", reference(claim));
        values.put("company", fallback(company, "the managing company"));
        values.put("previousStatus", fallback(previousStatus, "not available"));
        values.put("status", fallback(claim.getStatus(), "new"));
        values.put("previousItemState", fallback(previousItemState, "not available"));
        values.put("itemState", item == null ? "not available" : fallback(item.getState(), "not available"));
        values.put("submittedAt", formatDateTime(claim.getSubmittedAt()));
        values.put("updatedAt", formatDateTime(claim.getUpdatedAt()));
        values.put("passengerName", fallback(claim.getPassengerName(), "customer"));
        values.put("passengerEmail", fallback(claim.getPassengerEmail(), "not provided"));
        values.put("passengerPhone", fallback(claim.getPassengerPhone(), "not provided"));
        values.put("itemId", item == null || item.getId() == null ? "not available" : item.getId().toString());
        values.put("itemDescription", itemDescription(item));
        values.put("narrative", fallback(claim.getNarrative(), "not provided"));
        return values;
    }

    private static String render(String template, Map<String, String> placeholders) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    private static String reference(Claim claim) {
        return fallback(claim.getPublicReferenceCode(), shortId(claim.getId()));
    }

    private static String itemDescription(Item item) {
        if (item == null) {
            return "your selected lost item";
        }
        return fallback(item.getDescription(), "your selected lost item");
    }

    private static String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "not available";
        }
        return SUBMITTED_AT_FORMAT.format(dateTime);
    }

    private static String shortId(UUID id) {
        if (id == null) {
            return "not available";
        }
        return id.toString().substring(0, 8).toUpperCase();
    }

    private static String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public record ClaimEmailText(String subject, String body) {}
}