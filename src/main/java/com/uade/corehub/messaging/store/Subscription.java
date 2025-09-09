package com.uade.corehub.messaging.store;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que representa una suscripción a un tópico de mensajería
 * Permite a los squads suscribirse a eventos específicos con soporte para wildcards
 */
@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    /**
     * Identificador único de la suscripción (UUID)
     */
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    /**
     * URL del webhook donde se enviarán las notificaciones
     */
    @Column(name = "webhook_url", nullable = false, length = 500)
    private String webhookUrl;

    /**
     * Nombre del squad que se suscribe al tópico
     * Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)
     */
    @Column(name = "squad_name", nullable = false, length = 100)
    private String squadName;

    /**
     * Tópico al que se suscribe
     * Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)
     */
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    /**
     * Evento específico al que se suscribe
     * Soporta wildcards: * (cualquier carácter) y # (cualquier secuencia de caracteres)
     */
    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    /**
     * Estado de la suscripción (ACTIVE, INACTIVE, SUSPENDED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    /**
     * Fecha y hora de creación de la suscripción
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Fecha y hora de la última actualización de la suscripción
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Número de intentos fallidos de entrega de webhook
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * Último error de entrega del webhook
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * Fecha y hora de la última entrega exitosa
     */
    @Column(name = "last_successful_delivery")
    private OffsetDateTime lastSuccessfulDelivery;

    /**
     * Pre-persist: Generar ID único y establecer fechas
     */
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = SubscriptionStatus.ACTIVE;
        }
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Pre-update: Actualizar fecha de modificación
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Enum para el estado de la suscripción
     */
    public enum SubscriptionStatus {
        ACTIVE,     // Suscripción activa y funcionando
        INACTIVE,   // Suscripción desactivada temporalmente
        SUSPENDED   // Suscripción suspendida por errores
    }
}
