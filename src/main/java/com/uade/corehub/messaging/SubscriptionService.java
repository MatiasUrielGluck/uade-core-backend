package com.uade.corehub.messaging;

import com.uade.corehub.messaging.dto.SubscriptionRequest;
import com.uade.corehub.messaging.dto.SubscriptionResponse;
import com.uade.corehub.messaging.store.Subscription;
import com.uade.corehub.messaging.store.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Servicio para manejar las suscripciones a tópicos de mensajería
 * Proporciona funcionalidades para crear, buscar y gestionar suscripciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Crea una nueva suscripción a un tópico
     * Valida los datos de entrada y persiste la suscripción en la base de datos
     * 
     * @param request datos de la suscripción a crear
     * @return respuesta con el ID de la suscripción creada
     * @throws IllegalArgumentException si los datos no son válidos
     */
    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        log.info("Creando suscripción para squad: {}, tópico: {}, evento: {}", 
                request.squadName(), request.topic(), request.eventName());

        // Validar que los wildcards se usen correctamente
        if (!request.hasValidWildcards()) {
            String errorMsg = "Los wildcards # solo pueden usarse al inicio o final del patrón";
            log.warn("Error en wildcards: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Verificar si ya existe una suscripción duplicada
        if (subscriptionRepository.existsByWebhookUrlAndTopic(request.webhookUrl(), request.topic())) {
            String errorMsg = "Ya existe una suscripción con la misma URL de webhook y tópico";
            log.warn("Suscripción duplicada: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Crear la entidad de suscripción
        Subscription subscription = Subscription.builder()
                .webhookUrl(request.webhookUrl())
                .squadName(request.squadName())
                .topic(request.topic())
                .eventName(request.eventName())
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .build();

        // Persistir en la base de datos
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        
        log.info("Suscripción creada exitosamente con ID: {}", savedSubscription.getId());
        
        return SubscriptionResponse.fromSubscription(savedSubscription);
    }

    /**
     * Busca una suscripción por su ID
     * 
     * @param subscriptionId ID de la suscripción a buscar
     * @return Optional con la suscripción si existe
     */
    public Optional<Subscription> findSubscriptionById(String subscriptionId) {
        log.debug("Buscando suscripción con ID: {}", subscriptionId);
        return subscriptionRepository.findById(subscriptionId);
    }

    /**
     * Busca todas las suscripciones activas
     * 
     * @return Lista de suscripciones activas
     */
    public List<Subscription> findActiveSubscriptions() {
        log.debug("Buscando todas las suscripciones activas");
        return subscriptionRepository.findByStatus(Subscription.SubscriptionStatus.ACTIVE);
    }

    /**
     * Busca suscripciones que coincidan con un tópico y evento específicos
     * Utiliza el sistema de wildcards para encontrar coincidencias
     * 
     * @param topic tópico a buscar
     * @param eventName nombre del evento a buscar
     * @return Lista de suscripciones que coinciden
     */
    public List<Subscription> findMatchingSubscriptions(String topic, String eventName) {
        log.debug("Buscando suscripciones que coincidan con tópico: {} y evento: {}", topic, eventName);
        
        List<Subscription> allActiveSubscriptions = findActiveSubscriptions();
        
        return allActiveSubscriptions.stream()
                .filter(subscription -> matchesPattern(subscription.getTopic(), topic))
                .filter(subscription -> matchesPattern(subscription.getEventName(), eventName))
                .toList();
    }

    /**
     * Verifica si un valor coincide con un patrón que puede contener wildcards
     * 
     * @param pattern patrón con wildcards (* y #)
     * @param value valor a verificar
     * @return true si el valor coincide con el patrón
     */
    private boolean matchesPattern(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }
        
        // Si el patrón no tiene wildcards, comparación exacta
        if (!pattern.contains("*") && !pattern.contains("#")) {
            return pattern.equals(value);
        }
        
        // Convertir patrón con wildcards a regex
        String regex = SubscriptionRequest.wildcardToRegex(pattern);
        
        try {
            Pattern compiledPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            return compiledPattern.matcher(value).matches();
        } catch (Exception e) {
            log.warn("Error al compilar regex para patrón: {}", pattern, e);
            return false;
        }
    }

    /**
     * Busca suscripciones por nombre del squad
     * 
     * @param squadName nombre del squad
     * @return Lista de suscripciones del squad
     */
    public List<Subscription> findSubscriptionsBySquad(String squadName) {
        log.debug("Buscando suscripciones para squad: {}", squadName);
        return subscriptionRepository.findBySquadName(squadName);
    }

    /**
     * Actualiza el estado de una suscripción
     * 
     * @param subscriptionId ID de la suscripción
     * @param status nuevo estado
     * @return true si se actualizó correctamente
     */
    @Transactional
    public boolean updateSubscriptionStatus(String subscriptionId, Subscription.SubscriptionStatus status) {
        log.info("Actualizando estado de suscripción {} a: {}", subscriptionId, status);
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            log.warn("Suscripción no encontrada: {}", subscriptionId);
            return false;
        }
        
        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(status);
        subscriptionRepository.save(subscription);
        
        log.info("Estado de suscripción {} actualizado exitosamente", subscriptionId);
        return true;
    }

    /**
     * Elimina una suscripción
     * 
     * @param subscriptionId ID de la suscripción a eliminar
     * @return true si se eliminó correctamente
     */
    @Transactional
    public boolean deleteSubscription(String subscriptionId) {
        log.info("Eliminando suscripción: {}", subscriptionId);
        
        if (!subscriptionRepository.existsById(subscriptionId)) {
            log.warn("Suscripción no encontrada para eliminar: {}", subscriptionId);
            return false;
        }
        
        subscriptionRepository.deleteById(subscriptionId);
        log.info("Suscripción {} eliminada exitosamente", subscriptionId);
        return true;
    }

    /**
     * Cuenta el número de suscripciones activas por squad
     * 
     * @param squadName nombre del squad
     * @return número de suscripciones activas
     */
    public long countActiveSubscriptionsBySquad(String squadName) {
        return subscriptionRepository.countBySquadNameAndStatus(squadName, Subscription.SubscriptionStatus.ACTIVE);
    }
}
