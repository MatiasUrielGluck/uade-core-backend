package com.uade.corehub.messaging;

import com.uade.corehub.channels.ChannelRegistry;
import com.uade.corehub.channels.ChannelRegistryProperties;
import com.uade.corehub.messaging.infrastructure.RabbitMQInfrastructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador para la gestión de canales usando el patrón de topics de RabbitMQ
 */
@Slf4j
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
@Tag(name = "Gestión de Canales", description = "Endpoints para gestionar canales y infraestructura de mensajería")
public class ChannelManagementController {

    private final ChannelRegistry channelRegistry;
    private final RabbitMQInfrastructureService infrastructureService;

    @GetMapping("/{channelName}/status")
    @Operation(summary = "Verificar estado del canal")
    public ResponseEntity<Map<String, Object>> getChannelStatus(@PathVariable String channelName) {
        Optional<ChannelRegistryProperties.Channel> channel = channelRegistry.find(channelName);
        
        if (channel.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean infrastructureReady = infrastructureService.isInfrastructureReady(channelName);
        
        Map<String, Object> status = Map.of(
            "channelName", channelName,
            "exists", true,
            "exchange", channel.get().getExchange(),
            "routingKey", channel.get().getRoutingKey(),
            "infrastructureReady", infrastructureReady
        );

        return ResponseEntity.ok(status);
    }

    @PostMapping("/{channelName}/infrastructure")
    @Operation(summary = "Crear infraestructura del canal")
    public ResponseEntity<Map<String, Object>> createChannelInfrastructure(@PathVariable String channelName) {
        Optional<ChannelRegistryProperties.Channel> channel = channelRegistry.find(channelName);
        
        if (channel.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            boolean success = infrastructureService.ensureInfrastructureForChannel(channelName);
            
            Map<String, Object> result = Map.of(
                "channelName", channelName,
                "success", success,
                "message", success ? "Infraestructura creada exitosamente" : "Error al crear la infraestructura"
            );

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error creating infrastructure for channel: {}", channelName, e);
            Map<String, Object> error = Map.of(
                "channelName", channelName,
                "success", false,
                "message", "Error al crear la infraestructura: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping
    @Operation(summary = "Listar canales")
    public ResponseEntity<Map<String, Object>> listChannels() {
        var allChannels = channelRegistry.getAllChannels();
        
        Map<String, Object> channelsMap = allChannels.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> Map.of(
                    "exchange", entry.getValue().getExchange(),
                    "routingKey", entry.getValue().getRoutingKey(),
                    "infrastructureReady", infrastructureService.isInfrastructureReady(entry.getKey())
                )
            ));

        Map<String, Object> response = Map.of(
            "channels", channelsMap,
            "totalCount", allChannels.size()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/infrastructure/initialize")
    @Operation(summary = "Inicializar toda la infraestructura")
    public ResponseEntity<Map<String, Object>> initializeAllInfrastructure() {
        try {
            infrastructureService.initializeAllChannels();
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "Infraestructura inicializada para todos los canales"
            );

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error initializing all infrastructure", e);
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "Error al inicializar la infraestructura: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/architecture")
    @Operation(summary = "Explicar arquitectura de RabbitMQ")
    public ResponseEntity<Map<String, Object>> explainArchitecture() {
        Map<String, Object> explanation = Map.of(
            "pattern", "Topic Exchange con Colas Específicas",
            "description", "Creamos un exchange por squad y una cola por evento con bindings específicos",
            "example", Map.of(
                "squad", "payments",
                "events", java.util.List.of(
                    "payments.order.created",
                    "payments.order.canceled", 
                    "payments.payment.processed"
                ),
                "infrastructure", Map.of(
                    "exchange", "corehub.x.payments",
                    "queues", java.util.List.of(
                        "payments.order.created",
                        "payments.order.canceled",
                        "payments.payment.processed"
                    ),
                    "bindings", java.util.List.of(
                        "payments.order.created -> payments.order.created",
                        "payments.order.canceled -> payments.order.canceled",
                        "payments.payment.processed -> payments.payment.processed"
                    )
                )
            ),
            "benefits", java.util.List.of(
                "Routing específico por evento",
                "Colas independientes para cada evento",
                "Escalabilidad por squad",
                "Sigue las mejores prácticas de RabbitMQ"
            ),
            "reference", "https://www.rabbitmq.com/tutorials/tutorial-five-java.html"
        );

        return ResponseEntity.ok(explanation);
    }

    @GetMapping("/debug/infrastructure")
    @Operation(summary = "Diagnóstico de infraestructura")
    public ResponseEntity<Map<String, Object>> debugInfrastructure() {
        var allChannels = channelRegistry.getAllChannels();
        
        Map<String, Object> debugInfo = allChannels.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> Map.of(
                    "channelName", entry.getKey(),
                    "exchange", entry.getValue().getExchange(),
                    "routingKey", entry.getValue().getRoutingKey(),
                    "infrastructureReady", infrastructureService.isInfrastructureReady(entry.getKey()),
                    "note", "Verifica en RabbitMQ GUI: Exchanges, Queues y Bindings"
                )
            ));

        Map<String, Object> response = Map.of(
            "totalChannels", allChannels.size(),
            "channels", debugInfo,
            "instructions", java.util.List.of(
                "1. Verifica en RabbitMQ GUI (http://localhost:15672) que existan los exchanges",
                "2. Verifica que existan las colas con los nombres exactos",
                "3. Verifica que existan los bindings entre exchanges y colas",
                "4. Si no existen, reinicia la aplicación para que se creen automáticamente"
            )
        );

        return ResponseEntity.ok(response);
    }

}
