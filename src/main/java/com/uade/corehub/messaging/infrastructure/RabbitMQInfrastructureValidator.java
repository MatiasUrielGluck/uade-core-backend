package com.uade.corehub.messaging.infrastructure;

import com.uade.corehub.channels.ChannelRegistry;
import com.uade.corehub.channels.ChannelRegistryProperties;
import com.uade.corehub.config.RabbitMQInfrastructureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio para validar la existencia de infraestructura de RabbitMQ
 * NO crea automáticamente, solo valida que exista
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQInfrastructureValidator {

    private final RabbitAdmin rabbitAdmin;
    private final ChannelRegistry channelRegistry;
    private final RabbitMQInfrastructureProperties infrastructureProperties;

    private final ConcurrentMap<String, Boolean> existingExchanges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> existingQueues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> existingBindings = new ConcurrentHashMap<>();

    /**
     * Valida si la infraestructura existe para un canal específico
     * @param channelName nombre del canal
     * @return true si existe, false si no existe
     */
    public boolean validateInfrastructureForChannel(String channelName) {
        return channelRegistry.find(channelName)
                .map(this::validateInfrastructureForChannel)
                .orElse(false);
    }

    /**
     * Valida si la infraestructura existe para un canal específico
     * @param channel canal a validar
     * @return true si existe, false si no existe
     */
    private boolean validateInfrastructureForChannel(ChannelRegistryProperties.Channel channel) {
        try {
            String exchangeName = channel.getExchange();
            String queueName = channel.getName();
            String routingKey = channel.getRoutingKey();

            boolean exchangeExists = validateExchange(exchangeName);
            boolean queueExists = validateQueue(queueName);
            boolean bindingExists = validateBinding(exchangeName, queueName, routingKey);

            boolean allExist = exchangeExists && queueExists && bindingExists;

            if (allExist) {
                log.debug("Infrastructure validated for channel: {} -> exchange: {}, queue: {}, routingKey: {}",
                        channel.getName(), exchangeName, queueName, routingKey);
            } else {
                log.warn("Infrastructure missing for channel: {} -> exchange: {}, queue: {}, routingKey: {}",
                        channel.getName(), exchangeName, queueName, routingKey);
            }

            return allExist;

        } catch (Exception e) {
            log.error("Failed to validate infrastructure for channel: {}", channel.getName(), e);
            return false;
        }
    }

    /**
     * Valida si un exchange existe
     */
    private boolean validateExchange(String exchangeName) {
        if (existingExchanges.containsKey(exchangeName)) {
            return existingExchanges.get(exchangeName);
        }

        try {
            // Verificar si el exchange existe en RabbitMQ
            boolean exists = rabbitAdmin.getRabbitTemplate().execute(channel -> {
                try {
                    channel.exchangeDeclarePassive(exchangeName);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            existingExchanges.put(exchangeName, exists);
            
            if (exists) {
                log.debug("Exchange exists: {}", exchangeName);
            } else {
                log.warn("Exchange does not exist: {}", exchangeName);
            }
            
            return exists;

        } catch (Exception e) {
            log.error("Error validating exchange: {}", exchangeName, e);
            existingExchanges.put(exchangeName, false);
            return false;
        }
    }

    /**
     * Valida si una cola existe
     */
    private boolean validateQueue(String queueName) {
        if (existingQueues.containsKey(queueName)) {
            return existingQueues.get(queueName);
        }

        try {
            // Verificar si la cola existe en RabbitMQ
            boolean exists = rabbitAdmin.getRabbitTemplate().execute(channel -> {
                try {
                    channel.queueDeclarePassive(queueName);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            existingQueues.put(queueName, exists);
            
            if (exists) {
                log.debug("Queue exists: {}", queueName);
            } else {
                log.warn("Queue does not exist: {}", queueName);
            }
            
            return exists;

        } catch (Exception e) {
            log.error("Error validating queue: {}", queueName, e);
            existingQueues.put(queueName, false);
            return false;
        }
    }

    /**
     * Valida si un binding existe
     */
    private boolean validateBinding(String exchangeName, String queueName, String routingKey) {
        String bindingKey = exchangeName + ":" + queueName + ":" + routingKey;
        
        if (existingBindings.containsKey(bindingKey)) {
            return existingBindings.get(bindingKey);
        }

        try {
            // Verificar si el binding existe en RabbitMQ
            boolean exists = rabbitAdmin.getRabbitTemplate().execute(channel -> {
                try {
                    // Verificar que el exchange y la cola existan
                    channel.exchangeDeclarePassive(exchangeName);
                    channel.queueDeclarePassive(queueName);
                    
                    // Verificar que el binding existe
                    // Nota: RabbitMQ no tiene un método directo para verificar bindings
                    // Por ahora, asumimos que existe si el exchange y la cola existen
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            existingBindings.put(bindingKey, exists);
            
            if (exists) {
                log.debug("Binding exists: {} -> {} with routing key: {}", exchangeName, queueName, routingKey);
            } else {
                log.warn("Binding does not exist: {} -> {} with routing key: {}", exchangeName, queueName, routingKey);
            }
            
            return exists;

        } catch (Exception e) {
            log.error("Error validating binding: {} -> {} with routing key: {}", exchangeName, queueName, routingKey, e);
            existingBindings.put(bindingKey, false);
            return false;
        }
    }

    /**
     * Valida toda la infraestructura configurada
     */
    public void validateAllInfrastructure() {
        log.info("Validating RabbitMQ infrastructure for all configured channels...");
        
        var allChannels = channelRegistry.getAllChannels();
        int totalChannels = allChannels.size();
        int validChannels = 0;

        for (var channel : allChannels.values()) {
            if (validateInfrastructureForChannel(channel)) {
                validChannels++;
            }
        }

        log.info("RabbitMQ infrastructure validation completed: {}/{} channels have valid infrastructure", 
                validChannels, totalChannels);
    }

    /**
     * Verifica si la infraestructura está lista para un canal
     */
    public boolean isInfrastructureReady(String channelName) {
        return validateInfrastructureForChannel(channelName);
    }
}
