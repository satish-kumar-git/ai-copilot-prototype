package com.aiprototype.engine.config;

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
                        .title("AI Copilot Prototype — Requirement Engine")
                        .version("1.0.0")
                        .description("Analyses software requirements and returns structured engineering plans " +
                                     "with AI prompt hints for each task.")
                        .contact(new Contact()
                                .name("Engineering")
                                .email("engineering@aiprototype.com")));
    }
}
