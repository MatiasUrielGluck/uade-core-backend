package com.uade.corehub.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO para la respuesta del endpoint /list
 * Contiene un resumen de todas las suscripciones activas
 */
@Schema(
    description = "Resumen de todas las suscripciones activas",
    example = """
    {
      "totalSubscriptions": 3,
      "activeSubscriptions": 3,
      "events": [
        {
          "topic": "payments.order.created",
          "eventName": "orderCreated",
          "squadName": "payments-squad",
          "webhookUrl": "https://payments-squad.com/webhook"
        }
      ]
    }
    """
)
public record SubscriptionListResponse(
    @JsonProperty("totalSubscriptions")
    @Schema(description = "Número total de suscripciones", example = "3")
    int totalSubscriptions,
    
    @JsonProperty("activeSubscriptions")
    @Schema(description = "Número de suscripciones activas", example = "3")
    int activeSubscriptions,
    
    @JsonProperty("events")
    @Schema(description = "Lista de eventos suscritos")
    List<SubscriptionEventSummary> events
) {}
