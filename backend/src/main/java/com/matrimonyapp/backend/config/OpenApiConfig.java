package com.matrimonyapp.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI matrimonyOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("MatrimonyApp Production REST API")
                        .description("High-security REST API for MatrimonyApp featuring Argon2id/BCrypt password protection, TOTP MFA, end-to-end verified matrimonial profiles, privacy controls, discovery search, and administrative governance.")
                        .version("1.0.0")
                        .contact(new Contact().name("MatrimonyApp Security Team").email("security@matrimonyapp.com"))
                        .license(new License().name("Proprietary").url("https://matrimonyapp.com/terms")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
