package com.uade.corehub.messaging.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para manejar las operaciones de base de datos de las suscripciones
 * Proporciona métodos para buscar, crear, actualizar y eliminar suscripciones
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * Busca una suscripción por su ID
     * @param id identificador único de la suscripción
     * @return Optional con la suscripción si existe
     */
    Optional<Subscription> findById(String id);

    /**
     * Busca todas las suscripciones activas
     * @return Lista de suscripciones con estado ACTIVE
     */
    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);

    /**
     * Busca suscripciones por nombre del squad
     * @param squadName nombre del squad
     * @return Lista de suscripciones del squad
     */
    List<Subscription> findBySquadName(String squadName);

    /**
     * Busca suscripciones por tópico
     * @param topic tópico de la suscripción
     * @return Lista de suscripciones para el tópico
     */
    List<Subscription> findByTopic(String topic);

    /**
     * Busca suscripciones que coincidan con un tópico y evento específicos
     * Utiliza consulta personalizada para manejar wildcards
     * @param topic tópico a buscar
     * @param eventName nombre del evento a buscar
     * @return Lista de suscripciones que coinciden
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND " +
           "(s.topic = :topic OR s.topic LIKE '%#%' OR s.topic LIKE '%*%') AND " +
           "(s.eventName = :eventName OR s.eventName LIKE '%#%' OR s.eventName LIKE '%*%')")
    List<Subscription> findMatchingSubscriptions(@Param("topic") String topic, @Param("eventName") String eventName);

    /**
     * Busca suscripciones activas que coincidan con un tópico específico
     * @param topic tópico exacto a buscar
     * @return Lista de suscripciones activas para el tópico
     */
    List<Subscription> findByTopicAndStatus(String topic, Subscription.SubscriptionStatus status);

    /**
     * Verifica si existe una suscripción con la misma URL de webhook y tópico
     * @param webhookUrl URL del webhook
     * @param topic tópico de la suscripción
     * @return true si existe una suscripción duplicada
     */
    boolean existsByWebhookUrlAndTopic(String webhookUrl, String topic);

    /**
     * Cuenta el número de suscripciones activas por squad
     * @param squadName nombre del squad
     * @return número de suscripciones activas
     */
    long countBySquadNameAndStatus(String squadName, Subscription.SubscriptionStatus status);
}
