package com.uade.corehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Hub Backend API")
                        .description("Sistema de mensajería con RabbitMQ para Core Hub")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Core Hub Team")
                                .email("support@corehub.com")
                                .url("https://corehub.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://arreglaya-core-backend.us-east-1.elasticbeanstalk.com")
                                .description("Cloud Production Server")
                ));
    }
}