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
                "SECRET_ACCESS_KEY")) {
            JsonNode value = settings.get(key);
            if (value != null && (value.isTextual() || value.isNumber())) {
                defaults.put(key, value.asText());
            }
        }
        return defaults;
    }
}
