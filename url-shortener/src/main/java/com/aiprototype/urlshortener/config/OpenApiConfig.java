package com.aiprototype.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Copilot Prototype — URL Shortener")
                .version("1.0.0")
                .description("""
                    Scalable URL shortener service with analytics.
                    Built end-to-end using AI-assisted development guided by the Requirement Engine.

                    Features:
                    - Create short URLs (auto-generated or custom code)
                    - HTTP 302 redirect with click tracking
                    - Per-URL analytics (total clicks, daily breakdown)
                    - Soft-delete with audit trail
                    - Optional expiry date-time
                    """)
                .contact(new Contact()
                    .name("Engineering")
                    .email("engineering@aiprototype.com")));
    }
}
