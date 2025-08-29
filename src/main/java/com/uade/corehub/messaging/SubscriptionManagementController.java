package com.uade.corehub.messaging;

import com.uade.corehub.messaging.dto.SubscriptionListResponse;
import com.uade.corehub.messaging.dto.SubscriptionEventSummary;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar suscripciones
 * Proporciona endpoints independientes para listar y eliminar suscripciones
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Gestión de Suscripciones", description = "Endpoints independientes para gestionar suscripciones")
public class SubscriptionManagementController {

    private final SubscriptionService subscriptionService;

    /**
     * Endpoint para listar todos los eventos a los que el servicio está suscrito
     * GET /list
     * 
     * @return resumen de todos los eventos suscritos
     */
    @Operation(
        summary = "Listar eventos suscritos",
        description = "Muestra un resumen de todos los eventos a los que el servicio está suscrito actualmente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de eventos suscritos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SubscriptionListResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Lista de eventos",
                        value = """
                        {
                          "totalSubscriptions": 3,
                          "activeSubscriptions": 3,
                          "events": [
                            {
                              "topic": "payments.order.created",
                              "eventName": "orderCreated",
                              "squadName": "payments-squad",
                              "webhookUrl": "https://payments-squad.com/webhook"
                            },
                            {
                              "topic": "payments.order.*",
                              "eventName": "order*",
                              "squadName": "notifications-squad",
                              "webhookUrl": "https://notifications-squad.com/webhook"
                            },
                            {
                              "topic": "#.order.#",
                              "eventName": "#",
                              "squadName": "analytics-squad",
                              "webhookUrl": "https://analytics-squad.com/webhook"
                            }
                          ]
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Sin suscripciones",
                        value = """
                        {
                          "totalSubscriptions": 0,
                          "activeSubscriptions": 0,
                          "events": []
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    @GetMapping("/list")
    public ResponseEntity<SubscriptionListResponse> listSubscribedEvents() {
        try {
            log.debug("Listando eventos suscritos");
            
            List<Subscription> subscriptions = subscriptionService.findActiveSubscriptions();
            
            List<SubscriptionEventSummary> events = subscriptions.stream()
                    .map(sub -> new SubscriptionEventSummary(
                            sub.getTopic(),
                            sub.getEventName(),
                            sub.getSquadName(),
                            sub.getWebhookUrl()
                    ))
                    .toList();
            
            SubscriptionListResponse response = new SubscriptionListResponse(
                    subscriptions.size(),
                    (int) subscriptions.stream().filter(s -> s.getStatus() == Subscription.SubscriptionStatus.ACTIVE).count(),
                    events
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al listar eventos suscritos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para eliminar una suscripción
     * DELETE /unsubscribe/{subscriptionId}
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
    @DeleteMapping("/unsubscribe/{subscriptionId}")
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
}
