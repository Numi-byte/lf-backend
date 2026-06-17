package it.bz.sta.lf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClaimEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ClaimEmailNotificationService.class);
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    private final JavaMailSender mailSender;
    private final ClaimEmailTextService claimEmailTextService;
    private final CompanyAccessService companyAccess;
    private final ClaimEmailManagementService emailManagementService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String fromAddress;
    private final String clientId;
    private final String tenantId;
    private final String clientSecret;

    public ClaimEmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            ClaimEmailTextService claimEmailTextService,
            CompanyAccessService companyAccess,
            ClaimEmailManagementService emailManagementService,
            ObjectMapper objectMapper,
            @Value("${lostfound.claims.mail.enabled:true}") boolean enabled,
            @Value("${lostfound.claims.mail.from:noreply@suedtirolmobil.info}") String fromAddress,
            @Value("${lostfound.claims.mail.oauth.client-id:}") String clientId,
            @Value("${lostfound.claims.mail.oauth.tenant-id:}") String tenantId,
            @Value("${lostfound.claims.mail.oauth.client-secret:}") String clientSecret
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.claimEmailTextService = claimEmailTextService;
        this.companyAccess = companyAccess;
        this.emailManagementService = emailManagementService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
    }

    public void sendClaimCreatedNotifications(Claim claim) {
        if (!canSend(claim)) {
            return;
        }

        String company = companyAccess.itemCompany(claim.getItem());
        sendToClaimant(claim);
        sendToCompany(claim, company);
    }

    public void sendClaimUpdatedNotifications(Claim claim, String previousStatus, String previousItemState) {
        if (!canSend(claim)) {
            return;
        }

        String company = companyAccess.itemCompany(claim.getItem());
        sendUpdateToClaimant(claim, company, previousStatus, previousItemState);
        sendUpdateToCompany(claim, company, previousStatus, previousItemState);
    }

    private boolean canSend(Claim claim) {
        if (!enabled) {
            log.info("Claim email notifications are disabled; claimId={}", claim.getId());
            return false;
        }
        if (!hasGraphConfig() && mailSender == null) {
            log.warn("Claim email notifications are enabled but neither Microsoft Graph nor JavaMailSender is configured; claimId={}", claim.getId());
            return false;
        }
        return true;
    }

    private void sendToClaimant(Claim claim) {
        if (claim.getPassengerEmail() == null || claim.getPassengerEmail().isBlank()) {
            log.warn("Skipping claimant confirmation email because passengerEmail is missing; claimId={}", claim.getId());
            return;
        }

        String company = companyAccess.itemCompany(claim.getItem());
        ClaimEmailTextService.ClaimEmailText text = claimEmailTextService.claimantSubmitted(claim, company);
        send(claim.getPassengerEmail(), text.subject(), text.body(), claim, "claimant");
    }

    private void sendToCompany(Claim claim, String company) {
        String recipient = emailManagementService.recipientsForCompany(company);
        if (recipient == null || recipient.isBlank()) {
            log.warn("Skipping company claim notification because no recipient is configured for company={}; claimId={}", company, claim.getId());
            return;
        }

        ClaimEmailTextService.ClaimEmailText text = claimEmailTextService.companyReceived(claim, company);
        send(recipient, text.subject(), text.body(), claim, "company");
    }

    private void sendUpdateToClaimant(Claim claim, String company, String previousStatus, String previousItemState) {
        if (claim.getPassengerEmail() == null || claim.getPassengerEmail().isBlank()) {
            log.warn("Skipping claimant update email because passengerEmail is missing; claimId={}", claim.getId());
            return;
        }

        ClaimEmailTextService.ClaimEmailText text = claimEmailTextService.claimantUpdated(claim, company, previousStatus, previousItemState);
        send(claim.getPassengerEmail(), text.subject(), text.body(), claim, "claimant update");
    }

    private void sendUpdateToCompany(Claim claim, String company, String previousStatus, String previousItemState) {
        String recipient = emailManagementService.recipientsForCompany(company);
        if (recipient == null || recipient.isBlank()) {
            log.warn("Skipping company claim update notification because no recipient is configured for company={}; claimId={}", company, claim.getId());
            return;
        }

        ClaimEmailTextService.ClaimEmailText text = claimEmailTextService.companyUpdated(claim, company, previousStatus, previousItemState);
        send(recipient, text.subject(), text.body(), claim, "company update");
    }

    private void send(String to, String subject, String body, Claim claim, String recipientType) {
        String[] recipients = recipients(to);
        if (recipients.length == 0) {
            log.warn("Skipping {} claim email notification because no valid recipient is configured; claimId={}", recipientType, claim.getId());
            return;
        }

        try {
            if (hasGraphConfig()) {
                sendWithMicrosoftGraph(recipients, subject, body);
            } else {
                sendWithJavaMail(recipients, subject, body);
            }
            log.info("Sent {} claim email notification for claimId={} to={}", recipientType, claim.getId(), to);
        } catch (Exception e) {
            log.error("Could not send {} claim email notification for claimId={} to={}", recipientType, claim.getId(), to, e);
        }
    }

    private void sendWithJavaMail(String[] recipients, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipients);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendWithMicrosoftGraph(String[] recipients, String subject, String body) throws Exception {
        String accessToken = accessToken();
        Map<String, Object> payload = Map.of(
                "message", Map.of(
                        "subject", subject,
                        "body", Map.of(
                                "contentType", "Text",
                                "content", body
                        ),
                        "toRecipients", graphRecipients(recipients)
                ),
                "saveToSentItems", false
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0/users/" + encode(fromAddress) + "/sendMail"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new MailException("Microsoft Graph sendMail failed with status " + response.statusCode() + ": " + response.body()) {};
        }
    }

    private String accessToken() throws Exception {
        String form = "grant_type=client_credentials"
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&scope=" + encode(GRAPH_SCOPE);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/" + encode(tenantId) + "/oauth2/v2.0/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new MailException("Microsoft identity token request failed with status " + response.statusCode() + ": " + response.body()) {};
        }

        JsonNode tokenResponse = objectMapper.readTree(response.body());
        JsonNode accessToken = tokenResponse.get("access_token");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new MailException("Microsoft identity token response did not include access_token") {};
        }
        return accessToken.asText();
    }

    private List<Map<String, Map<String, String>>> graphRecipients(String[] recipients) {
        List<Map<String, Map<String, String>>> graphRecipients = new ArrayList<>();
        for (String recipient : recipients) {
            graphRecipients.add(Map.of("emailAddress", Map.of("address", recipient)));
        }
        return graphRecipients;
    }

    private String[] recipients(String to) {
        return java.util.Arrays.stream(to.split(","))
                .map(String::trim)
                .filter(recipient -> !recipient.isBlank())
                .toArray(String[]::new);
    }

    private boolean hasGraphConfig() {
        return hasText(clientId) && hasText(tenantId) && hasText(clientSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}