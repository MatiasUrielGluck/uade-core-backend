package com.uade.corehub.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para la petición de suscripción a un tópico
 * Contiene los 4 campos requeridos: webhookUrl, squadName, topic, eventName
 * Soporta wildcards (* y #) en squadName, topic y eventName
 */
@Schema(
    description = "Datos para crear una nueva suscripción a un tópico de mensajería",
    example = """
    {
      "webhookUrl": "https://payments-squad.com/webhook",
      "squadName": "payments-squad",
      "topic": "payments.order.created",
      "eventName": "orderCreated"
    }
    """
)
public record SubscriptionRequest(
    
    /**
     * URL del webhook donde se enviarán las notificaciones
     * Debe ser una URL válida y no estar vacía
     */
    @JsonProperty("webhookUrl")
    @Schema(
        description = "URL del webhook donde se enviarán las notificaciones",
        example = "https://payments-squad.com/webhook",
        maxLength = 500
    )
    @NotBlank(message = "La URL del webhook es obligatoria")
    @Pattern(regexp = "^(https?://)[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?$", 
             message = "La URL del webhook debe ser una URL válida")
    @Size(max = 500, message = "La URL del webhook no puede exceder 500 caracteres")
    String webhookUrl,
    
    /**
     * Nombre del squad que se suscribe al tópico
     * Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)
     */
    @JsonProperty("squadName")
    @Schema(
        description = "Nombre del squad que se suscribe al tópico. Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)",
        example = "payments-squad",
        minLength = 1,
        maxLength = 100
    )
    @NotBlank(message = "El nombre del squad es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre del squad debe tener entre 1 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_.#*]+$", 
             message = "El nombre del squad solo puede contener letras, números, guiones, puntos, # y *")
    String squadName,
    
    /**
     * Tópico al que se suscribe
     * Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)
     */
    @JsonProperty("topic")
    @Schema(
        description = "Tópico al que se suscribe. Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)",
        example = "payments.order.created",
        minLength = 1,
        maxLength = 200
    )
    @NotBlank(message = "El tópico es obligatorio")
    @Size(min = 1, max = 200, message = "El tópico debe tener entre 1 y 200 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_.:#*]+$", 
             message = "El tópico solo puede contener letras, números, guiones, puntos, dos puntos, # y *")
    String topic,
    
    /**
     * Evento específico al que se suscribe
     * Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)
     */
    @JsonProperty("eventName")
    @Schema(
        description = "Evento específico al que se suscribe. Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)",
        example = "orderCreated",
        minLength = 1,
        maxLength = 100
    )
    @NotBlank(message = "El nombre del evento es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre del evento debe tener entre 1 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_.#*]+$", 
             message = "El nombre del evento solo puede contener letras, números, guiones, puntos, # y *")
    String eventName
) {
    
    /**
     * Valida que los wildcards se usen correctamente
     * @return true si los wildcards son válidos
     */
    public boolean hasValidWildcards() {
        // Validar que # no esté en medio de una palabra (solo al final o principio)
        if (squadName.contains("#") && !squadName.matches("^#.*|.*#$|^#$")) {
            return false;
        }
        if (topic.contains("#") && !topic.matches("^#.*|.*#$|^#$")) {
            return false;
        }
        if (eventName.contains("#") && !eventName.matches("^#.*|.*#$|^#$")) {
            return false;
        }
        return true;
    }
    
    /**
     * Convierte el patrón con wildcards a una expresión regular
     * @param pattern patrón con wildcards
     * @return expresión regular equivalente
     */
    public static String wildcardToRegex(String pattern) {
        if (pattern == null) {
            return ".*";
        }
        // Escapar caracteres especiales de regex excepto * y #
        String escaped = pattern.replaceAll("[\\\\^$.\\[\\]|()?+{}]", "\\\\$0");
        // Convertir * a .* (cualquier secuencia de caracteres)
        escaped = escaped.replace("*", ".*");
        // Convertir # a .* (cualquier secuencia de caracteres)
        escaped = escaped.replace("#", ".*");
        return "^" + escaped + "$";
    }
}
