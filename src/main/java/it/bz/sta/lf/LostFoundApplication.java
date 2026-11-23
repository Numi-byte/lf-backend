package it.bz.sta.lf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LostFoundApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LostFoundApplication.class);
        String secret = System.getenv("SECRET_ENV");
        if (secret != null) {
            application.setDefaultProperties(readSecretSettings(secret));
        }
        application.run(args);
    }

    private static Map<String, Object> readSecretSettings(String secret) {
        JsonNode settings;
        try {
            settings = new ObjectMapper().readTree(secret);
        } catch (JsonProcessingException ignored) {
            // Parser diagnostics can contain secret values.
            throw new IllegalArgumentException("SECRET_ENV must contain a JSON object");
        }
        if (settings == null || !settings.isObject()) {
            throw new IllegalArgumentException("SECRET_ENV must contain a JSON object");
        }

        Map<String, Object> defaults = new HashMap<>();
        for (String key : List.of("DATABASE_HOST", "DATABASE_PORT", "DATABASE_NAME",
                "DATABASE_USER", "DATABASE_PASSWORD", "S3_BUCKET_NAME", "ACCESS_KEY_ID",
                "SECRET_ACCESS_KEY", "LOSTFOUND_CLAIMS_MAIL_ENABLED", "LOSTFOUND_CLAIMS_MAIL_FROM",
                "LOSTFOUND_CLAIMS_MAIL_OAUTH_CLIENT_ID", "LOSTFOUND_CLAIMS_MAIL_OAUTH_TENANT_ID",
                "LOSTFOUND_CLAIMS_MAIL_OAUTH_CLIENT_SECRET")) {
            JsonNode value = settings.get(key);
            if (value != null && (value.isTextual() || value.isNumber() || value.isBoolean())) {
                defaults.put(key, value.asText());
            }
        }
        Map.of(
                "SPRING_MAIL_HOST", "spring.mail.host",
                "SPRING_MAIL_PORT", "spring.mail.port",
                "SPRING_MAIL_USERNAME", "spring.mail.username",
                "SPRING_MAIL_PASSWORD", "spring.mail.password",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH", "spring.mail.properties.mail.smtp.auth",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE", "spring.mail.properties.mail.smtp.starttls.enable",
                "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED", "spring.mail.properties.mail.smtp.starttls.required"
        ).forEach((key, property) -> {
            JsonNode value = settings.get(key);
            if (value != null && !value.isNull() && value.isValueNode()) {
                defaults.put(property, value.asText());
            }
        });
        return defaults;
    }
}
