package com.princekumar.itams.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI 3 document that springdoc serves at
 * {@code /v3/api-docs} and renders in Swagger UI at
 * {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI itamsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ITAMS — IT Asset Management System")
                .description("REST API for the IT Asset Management System (Project 1 of the ibs-portfolio).")
                .version("v1")
                .contact(new Contact().name("Prince Kumar").email("princekumar2002.de@gmail.com"))
                .license(new License().name("MIT")));
    }
}
