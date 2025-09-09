package com.uade.corehub.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uade.corehub.messaging.store.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * DTO para la respuesta de suscripción
 * Contiene el ID de la suscripción creada y información básica
 */
@Schema(
    description = "Respuesta de una operación de suscripción",
    example = """
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
)
public record SubscriptionResponse(
    
    /**
     * ID único de la suscripción creada
     */
    @JsonProperty("subscriptionId")
    String subscriptionId,
    
    /**
     * URL del webhook configurada
     */
    @JsonProperty("webhookUrl")
    String webhookUrl,
    
    /**
     * Nombre del squad suscrito
     */
    @JsonProperty("squadName")
    String squadName,
    
    /**
     * Tópico suscrito
     */
    @JsonProperty("topic")
    String topic,
    
    /**
     * Nombre del evento suscrito
     */
    @JsonProperty("eventName")
    String eventName,
    
    /**
     * Estado de la suscripción
     */
    @JsonProperty("status")
    String status,
    
    /**
     * Fecha y hora de creación de la suscripción
     */
    @JsonProperty("createdAt")
    OffsetDateTime createdAt,
    
    /**
     * Mensaje de confirmación
     */
    @JsonProperty("message")
    String message
) {
    
    /**
     * Constructor estático para crear una respuesta desde una entidad Subscription
     * @param subscription entidad de suscripción
     * @return SubscriptionResponse
     */
    public static SubscriptionResponse fromSubscription(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getWebhookUrl(),
            subscription.getSquadName(),
            subscription.getTopic(),
            subscription.getEventName(),
            subscription.getStatus().name(),
            subscription.getCreatedAt(),
            "Suscripción creada exitosamente"
        );
    }
    
    /**
     * Constructor estático para crear una respuesta de error
     * @param errorMessage mensaje de error
     * @return SubscriptionResponse con información de error
     */
    public static SubscriptionResponse error(String errorMessage) {
        return new SubscriptionResponse(
            null,
            null,
            null,
            null,
            null,
            "ERROR",
            OffsetDateTime.now(),
            errorMessage
        );
    }
}
