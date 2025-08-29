package com.uade.corehub.messaging;

import com.uade.corehub.messaging.dto.SubscriptionRequest;
import com.uade.corehub.messaging.dto.SubscriptionResponse;
import com.uade.corehub.messaging.store.Subscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Controlador REST para manejar las suscripciones a tópicos de mensajería
 * Proporciona endpoints para crear, consultar y gestionar suscripciones
 */
@Slf4j
@RestController
@RequestMapping("/subscribe")
@RequiredArgsConstructor
@Validated
@Tag(name = "Suscripciones", description = "Endpoints para gestionar suscripciones a tópicos de mensajería")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Endpoint para crear una nueva suscripción
     * POST /subscribe
     * 
     * @param request datos de la suscripción a crear
     * @return respuesta con el ID de la suscripción creada
     */
    @Operation(
        summary = "Crear suscripción",
        description = "Crea una nueva suscripción a un tópico de mensajería con soporte para wildcards"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Suscripción creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Suscripción básica",
                        value = """
                        {
                          "subscriptionId": "550e8400-e29b-41d4-a716-446655440000",
                          "webhookUrl": "https://payments-squad.com/webhook",
                          "squadName": "payments-squad",
                          "topic": "payments.order.created",
                          "eventName": "orderCreated",
                          "status": "ACTIVE",
                          "createdAt": "2025-08-28T21:55:00Z",
                          "message": "Suscripción creada exitosamente"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Suscripción con wildcards",
                        value = """
                        {
                          "subscriptionId": "550e8400-e29b-41d4-a716-446655440001",
                          "webhookUrl": "https://notifications-squad.com/webhook",
                          "squadName": "notifications-squad",
                          "topic": "payments.order.*",
                          "eventName": "order*",
                          "status": "ACTIVE",
                          "createdAt": "2025-08-28T21:55:00Z",
                          "message": "Suscripción creada exitosamente"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionResponse.class),
                examples = {
                    @ExampleObject(
                        name = "URL inválida",
                        value = """
                        {
                          "subscriptionId": null,
                          "webhookUrl": null,
                          "squadName": null,
                          "topic": null,
                          "eventName": null,
                          "status": "ERROR",
                          "createdAt": "2025-08-28T21:55:00Z",
                          "message": "La URL del webhook debe ser una URL válida"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Wildcard inválido",
                        value = """
                        {
                          "subscriptionId": null,
                          "webhookUrl": null,
                          "squadName": null,
                          "topic": null,
                          "eventName": null,
                          "status": "ERROR",
                          "createdAt": "2025-08-28T21:55:00Z",
                          "message": "Los wildcards # solo pueden usarse al inicio o final del patrón"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
        @Parameter(
            description = "Datos de la suscripción a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Suscripción básica",
                        value = """
                        {
                          "webhookUrl": "https://payments-squad.com/webhook",
                          "squadName": "payments-squad",
                          "topic": "payments.order.created",
                          "eventName": "orderCreated"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Suscripción con wildcard *",
                        value = """
                        {
                          "webhookUrl": "https://notifications-squad.com/webhook",
                          "squadName": "notifications-squad",
                          "topic": "payments.order.*",
                          "eventName": "order*"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Suscripción con wildcard #",
                        value = """
                        {
                          "webhookUrl": "https://analytics-squad.com/webhook",
                          "squadName": "analytics-squad",
                          "topic": "#.order.#",
                          "eventName": "#"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Suscripción para todos los eventos",
                        value = """
                        {
                          "webhookUrl": "https://audit-squad.com/webhook",
                          "squadName": "audit-squad",
                          "topic": "payments.*",
                          "eventName": "*"
                        }
                        """
                    )
                }
            )
        )
        @Valid @RequestBody SubscriptionRequest request
    ) {
        try {
            log.info("Recibida petición de suscripción - Squad: {}, Tópico: {}, Evento: {}", 
                    request.squadName(), request.topic(), request.eventName());

            SubscriptionResponse response = subscriptionService.createSubscription(request);
            
            log.info("Suscripción creada exitosamente con ID: {}", response.subscriptionId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación en suscripción: {}", e.getMessage());
            SubscriptionResponse errorResponse = SubscriptionResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error inesperado al crear suscripción", e);
            SubscriptionResponse errorResponse = SubscriptionResponse.error("Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Endpoint para obtener una suscripción por su ID
     * GET /subscribe/{subscriptionId}
     * 
     * @param subscriptionId ID de la suscripción a consultar
     * @return suscripción si existe, 404 si no existe
     */
    @Operation(
        summary = "Obtener suscripción por ID",
        description = "Recupera una suscripción específica por su identificador único"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Suscripción encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Suscripción no encontrada"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
        @Parameter(
            description = "ID único de la suscripción",
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable String subscriptionId
    ) {
        try {
            log.debug("Consultando suscripción con ID: {}", subscriptionId);
            
            Optional<Subscription> subscriptionOpt = subscriptionService.findSubscriptionById(subscriptionId);
            
            if (subscriptionOpt.isEmpty()) {
                log.warn("Suscripción no encontrada: {}", subscriptionId);
                return ResponseEntity.notFound().build();
            }
            
            Subscription subscription = subscriptionOpt.get();
            SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al consultar suscripción: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para obtener todas las suscripciones activas
     * GET /subscribe
     * 
     * @return lista de todas las suscripciones activas
     */
    @Operation(
        summary = "Obtener todas las suscripciones activas",
        description = "Recupera todas las suscripciones que están en estado ACTIVE"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de suscripciones activas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAllActiveSubscriptions() {
        try {
            log.debug("Consultando todas las suscripciones activas");
            
            List<Subscription> subscriptions = subscriptionService.findActiveSubscriptions();
            List<SubscriptionResponse> responses = subscriptions.stream()
                    .map(SubscriptionResponse::fromSubscription)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error al consultar suscripciones activas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para obtener suscripciones por squad
     * GET /subscribe/squad/{squadName}
     * 
     * @param squadName nombre del squad
     * @return lista de suscripciones del squad
     */
    @Operation(
        summary = "Obtener suscripciones por squad",
        description = "Recupera todas las suscripciones de un squad específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de suscripciones del squad",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @GetMapping("/squad/{squadName}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsBySquad(
        @Parameter(
            description = "Nombre del squad",
            example = "payments-squad"
        )
        @PathVariable String squadName
    ) {
        try {
            log.debug("Consultando suscripciones para squad: {}", squadName);
            
            List<Subscription> subscriptions = subscriptionService.findSubscriptionsBySquad(squadName);
            List<SubscriptionResponse> responses = subscriptions.stream()
                    .map(SubscriptionResponse::fromSubscription)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error al consultar suscripciones del squad: {}", squadName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para actualizar el estado de una suscripción
     * PUT /subscribe/{subscriptionId}/status
     * 
     * @param subscriptionId ID de la suscripción
     * @param status nuevo estado (ACTIVE, INACTIVE, SUSPENDED)
     * @return 200 si se actualizó correctamente, 404 si no existe
     */
    @Operation(
        summary = "Actualizar estado de suscripción",
        description = "Cambia el estado de una suscripción (ACTIVE, INACTIVE, SUSPENDED)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estado actualizado correctamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Suscripción no encontrada"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @PutMapping("/{subscriptionId}/status")
    public ResponseEntity<Void> updateSubscriptionStatus(
        @Parameter(
            description = "ID único de la suscripción",
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable String subscriptionId,
        @Parameter(
            description = "Nuevo estado de la suscripción",
            example = "ACTIVE",
            schema = @Schema(allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
        )
        @RequestParam Subscription.SubscriptionStatus status
    ) {
        
        try {
            log.info("Actualizando estado de suscripción {} a: {}", subscriptionId, status);
            
            boolean updated = subscriptionService.updateSubscriptionStatus(subscriptionId, status);
            
            if (updated) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error al actualizar estado de suscripción: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para eliminar una suscripción
     * DELETE /subscribe/{subscriptionId}
     * 
     * @param subscriptionId ID de la suscripción a eliminar
     * @return 200 si se eliminó correctamente, 404 si no existe
     */
    @Operation(
        summary = "Eliminar suscripción",
        description = "Elimina permanentemente una suscripción del sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Suscripción eliminada correctamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Suscripción no encontrada"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> deleteSubscription(
        @Parameter(
            description = "ID único de la suscripción a eliminar",
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable String subscriptionId
    ) {
        try {
            log.info("Eliminando suscripción: {}", subscriptionId);
            
            boolean deleted = subscriptionService.deleteSubscription(subscriptionId);
            
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error al eliminar suscripción: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para obtener estadísticas de suscripciones por squad
     * GET /subscribe/stats/squad/{squadName}
     * 
     * @param squadName nombre del squad
     * @return número de suscripciones activas del squad
     */
    @Operation(
        summary = "Obtener estadísticas por squad",
        description = "Cuenta el número de suscripciones activas de un squad específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Número de suscripciones activas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "integer"),
                examples = {
                    @ExampleObject(
                        name = "Squad con suscripciones",
                        value = "3"
                    ),
                    @ExampleObject(
                        name = "Squad sin suscripciones",
                        value = "0"
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @GetMapping("/stats/squad/{squadName}")
    public ResponseEntity<Long> getSubscriptionStatsBySquad(
        @Parameter(
            description = "Nombre del squad",
            example = "payments-squad"
        )
        @PathVariable String squadName
    ) {
        try {
            log.debug("Consultando estadísticas de suscripciones para squad: {}", squadName);
            
            long count = subscriptionService.countActiveSubscriptionsBySquad(squadName);
            
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            log.error("Error al consultar estadísticas del squad: {}", squadName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
