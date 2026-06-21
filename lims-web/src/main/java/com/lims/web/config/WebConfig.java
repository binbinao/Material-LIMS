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
        // Accept-header is included because some browsers also list it
        // explicitly in Access-Control-Request-Headers even when it is
        // a CORS-safelisted request header. X-Dev-User is required by
        // DevAuthFilter (see com.lims.web.security.DevAuthFilter) and
        // must be allowed or every dev-mode fetch will fail the
        // preflight with 403 "Invalid CORS request" — this silently
        // breaks the login page in dev.
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "X-Requested-With",
                        "X-Dev-User",
                        "Accept")
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
