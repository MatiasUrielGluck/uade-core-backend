package com.uade.corehub.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para representar un resumen de un evento suscrito
 * Usado en la respuesta del endpoint /list
 */
@Schema(
    description = "Resumen de un evento suscrito",
    example = """
    {
      "topic": "payments.order.created",
      "eventName": "orderCreated",
      "squadName": "payments-squad",
      "webhookUrl": "https://payments-squad.com/webhook"
    }
    """
)
public record SubscriptionEventSummary(
    @JsonProperty("topic")
    @Schema(description = "Tópico del evento", example = "payments.order.created")
    String topic,
    
    @JsonProperty("eventName")
    @Schema(description = "Nombre del evento", example = "orderCreated")
    String eventName,
    
    @JsonProperty("squadName")
    @Schema(description = "Nombre del squad suscrito", example = "payments-squad")
    String squadName,
    
    @JsonProperty("webhookUrl")
    @Schema(description = "URL del webhook", example = "https://payments-squad.com/webhook")
    String webhookUrl
) {}
