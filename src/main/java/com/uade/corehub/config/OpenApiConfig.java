package com.uade.corehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para documentar la API REST
 * Proporciona información sobre los endpoints, modelos y ejemplos de uso
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configuración principal de OpenAPI
     * Define la información general de la API y los servidores disponibles
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoreHub API - Sistema de Suscripciones")
                        .description("""
                                API para gestionar suscripciones a tópicos de mensajería con soporte para wildcards.
                                
                                ## Características principales:
                                - ✅ Crear, consultar y gestionar suscripciones
                                - ✅ Endpoint `GET /list` para listar eventos suscritos
                                - ✅ Endpoint `DELETE /unsubscribe/{id}` para eliminar suscripciones
                                - ✅ Soporte para wildcards (* y #) en tópicos y eventos
                                - ✅ Validaciones robustas de datos de entrada
                                - ✅ Persistencia en base de datos PostgreSQL
                                - ✅ Estados de suscripción (ACTIVE, INACTIVE, SUSPENDED)
                                
                                ## Wildcards soportados:
                                - `*` - Cualquier carácter
                                - `#` - Cualquier secuencia de caracteres (solo al inicio o final)
                                
                                ## Ejemplos de uso:
                                - `payments.order.*` - Cualquier evento de orden de pagos
                                - `#.order.#` - Cualquier orden de cualquier dominio
                                - `orderCreated` - Evento específico
                                - `order*` - Cualquier evento que empiece con "order"
                                """)
                        .version("0.0.1")
                        )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de desarrollo local")
                ));
    }
}
