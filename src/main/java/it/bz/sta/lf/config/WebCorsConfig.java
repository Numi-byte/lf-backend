// src/main/java/it/bz/sta/lf/config/WebCorsConfig.java
package it.bz.sta.lf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // React dev server
                .allowedOriginPatterns(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://192.168.*:*",
                        "http://10.*.*.*:*",
                        "http://172.16.*.*:*",
                        "http://172.17.*.*:*",
                        "http://172.18.*.*:*",
                        "http://172.19.*.*:*",
                        "http://172.2*.*.*:*",
                        "http://172.30.*.*:*",
                        "http://172.31.*.*:*"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")

                .allowCredentials(false)
                .maxAge(3600);
    }
}
