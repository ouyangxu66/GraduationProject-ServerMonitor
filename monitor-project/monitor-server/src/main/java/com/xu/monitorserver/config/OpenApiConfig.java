package com.xu.monitorserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Monitor Project API")
                        .version("0.1.0")
                        .description("Monitor Project API Documentation")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com")));
    }
}
