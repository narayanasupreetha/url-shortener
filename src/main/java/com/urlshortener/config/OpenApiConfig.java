package com.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the URL Shortener application.
 * This class customizes the generated Swagger documentation by:
 * - Defining API metadata (title, version, description, contact, license)
 * - Configuring JWT Bearer authentication
 * - Applying the security scheme globally to secured endpoints
 * It enables Swagger UI to support authenticated API testing.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("""
                                REST API for the URL Shortener service.
                                
                                **Authentication**
                                Most endpoints require a JWT Bearer token.
                                Obtain one by calling `POST /api/v1/auth/login`,
                                then click **Authorize** and paste the token
                                (without the `Bearer ` prefix).
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("URL Shortener Team"))
                        .license(new License()
                                .name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide the JWT token obtained from /api/v1/auth/login")));
    }
}
