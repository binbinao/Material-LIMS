package com.lims.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Issue #28: CORS allowed origins driven from a property so non-localhost
     *  deployments (https://lims.example.com etc.) work without a code change.
     *  Comma-separated. Defaults to localhost for local dev. */
    @Value("${cors.allowed-origins:http://localhost:8000,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
