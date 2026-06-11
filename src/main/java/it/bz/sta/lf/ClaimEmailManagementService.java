package it.bz.sta.lf;

import it.bz.sta.lf.dto.ClaimEmailSettingUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ClaimEmailManagementService {

    public static final String DEFAULT_CLAIMANT_SUBJECT_TEMPLATE = "Your lost-and-found claim {claimReference} was submitted";
    public static final String DEFAULT_CLAIMANT_BODY_TEMPLATE = "Dear {passengerName},\n\n"
            + "Thank you for submitting your lost-and-found claim. We have received it and our team will review the details.\n\n"
            + "Claim reference: {claimReference}\n"
            + "Item: {itemDescription}\n"
            + "Submitted at: {submittedAt}\n\n"
            + "Please keep this reference number for future communication. We will contact you if we need more information or when there is an update.\n\n"
            + "Kind regards,\n"
            + "Lost & Found Team";
    public static final String DEFAULT_COMPANY_SUBJECT_TEMPLATE = "New lost-and-found claim received: {claimReference}";
    public static final String DEFAULT_COMPANY_BODY_TEMPLATE = "A new lost-and-found claim has been received for {company}.\n\n"
            + "Claim reference: {claimReference}\n"
            + "Status: {status}\n"
            + "Submitted at: {submittedAt}\n\n"
            + "Claimant details\n"
            + "Name: {passengerName}\n"
            + "Email: {passengerEmail}\n"
            + "Phone: {passengerPhone}\n\n"
            + "Item details\n"
            + "Item ID: {itemId}\n"
            + "Description: {itemDescription}\n\n"
            + "Claim narrative\n"
            + "{narrative}\n\n"
            + "Please review the claim in the Lost & Found back office.";

    private static final Map<String, String> DEFAULT_COMPANY_RECIPIENTS = defaultCompanyRecipients();

    private final ClaimEmailSettingRepository settings;
    private final CompanyAccessService companyAccess;

    public ClaimEmailManagementService(ClaimEmailSettingRepository settings, CompanyAccessService companyAccess) {
        this.settings = settings;
        this.companyAccess = companyAccess;
    }

    public List<ClaimEmailSetting> listSettings() {
        Map<String, ClaimEmailSetting> merged = new LinkedHashMap<>();
        DEFAULT_COMPANY_RECIPIENTS.keySet().stream().sorted().forEach(company -> merged.put(company, defaultSetting(company)));
        settings.findAll().forEach(setting -> merged.put(normalizeCompany(setting.getCompany()), setting));
        return merged.values().stream()
                .sorted(Comparator.comparing(ClaimEmailSetting::getCompany))
                .toList();
    }

    public ClaimEmailSetting getSetting(String company) {
        String normalizedCompany = normalizeCompany(company);
        return settings.findById(normalizedCompany).orElseGet(() -> defaultSetting(normalizedCompany));
    }

    public String recipientsForCompany(String company) {
        return getSetting(company).getRecipients();
    }

    @Transactional
    public ClaimEmailSetting updateSetting(String company, ClaimEmailSettingUpdateRequest req, String updatedBy) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        String normalizedCompany = normalizeCompany(company);
        ClaimEmailSetting setting = settings.findById(normalizedCompany).orElseGet(() -> defaultSetting(normalizedCompany));
        setting.setCompany(normalizedCompany);
        setting.setRecipients(required(req.recipients(), "recipients"));
        setting.setClaimantSubjectTemplate(required(req.claimantSubjectTemplate(), "claimantSubjectTemplate"));
        setting.setClaimantBodyTemplate(required(req.claimantBodyTemplate(), "claimantBodyTemplate"));
        setting.setCompanySubjectTemplate(required(req.companySubjectTemplate(), "companySubjectTemplate"));
        setting.setCompanyBodyTemplate(required(req.companyBodyTemplate(), "companyBodyTemplate"));
        setting.setUpdatedAt(OffsetDateTime.now());
        setting.setUpdatedBy(updatedBy);
        return settings.save(setting);
    }

    public static Map<String, String> defaultCompanyRecipients() {
        Map<String, String> recipients = new LinkedHashMap<>();
        recipients.put("sasa", "sasabz@sasabz.it");
        recipients.put("sasabz", "sasabz@sasabz.it");
        recipients.put("ksm", "info@ksm.bz.it");
        recipients.put("pizzinini", "info@pizzinini.it");
        recipients.put("taferner", "info@taferner.it");
        recipients.put("autorainer", "office@auto-rainer.com");
        recipients.put("auto-rainer", "office@auto-rainer.com");
        recipients.put("simobil", "info@silbernagl.it");
        recipients.put("silbernagl", "info@silbernagl.it");
        recipients.put("kronplatz", "mobility@kronplatz.group");
        recipients.put("sad", "info@sad.it");
        recipients.put("holzer", "info@holzer.eu");
        recipients.put("trenitalia", "customer.room.bolzano@trenitalia.it");
        recipients.put("sta", "infopoint-bz@sta.bz.it");
        return java.util.Collections.unmodifiableMap(recipients);
    }

    private ClaimEmailSetting defaultSetting(String company) {
        String normalizedCompany = normalizeCompany(company);
        ClaimEmailSetting setting = new ClaimEmailSetting();
        setting.setCompany(normalizedCompany);
        setting.setRecipients(DEFAULT_COMPANY_RECIPIENTS.getOrDefault(normalizedCompany, ""));
        setting.setClaimantSubjectTemplate(DEFAULT_CLAIMANT_SUBJECT_TEMPLATE);
        setting.setClaimantBodyTemplate(DEFAULT_CLAIMANT_BODY_TEMPLATE);
        setting.setCompanySubjectTemplate(DEFAULT_COMPANY_SUBJECT_TEMPLATE);
        setting.setCompanyBodyTemplate(DEFAULT_COMPANY_BODY_TEMPLATE);
        setting.setUpdatedAt(OffsetDateTime.now());
        return setting;
    }

    private String normalizeCompany(String company) {
        String normalized = companyAccess.normalizeCompany(company);
        if (normalized == null || normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "company is required");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required"));
    }
}