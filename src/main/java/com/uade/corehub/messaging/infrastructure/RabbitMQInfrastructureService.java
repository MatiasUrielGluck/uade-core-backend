package com.uade.corehub.messaging.infrastructure;

import com.uade.corehub.channels.ChannelRegistry;
import com.uade.corehub.channels.ChannelRegistryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio para la gestión de infraestructura de RabbitMQ
 * Crea un exchange por squad y una cola por evento con bindings específicos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQInfrastructureService {

    private final RabbitAdmin rabbitAdmin;
    private final ChannelRegistry channelRegistry;
    
    // Cache para evitar recrear infraestructura existente
    private final ConcurrentMap<String, Boolean> createdExchanges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> createdQueues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> createdBindings = new ConcurrentHashMap<>();

    /**
     * Asegura que la infraestructura necesaria para un canal esté creada
     * Crea un exchange por squad y una cola por evento con binding específico
     */
    public boolean ensureInfrastructureForChannel(String channelName) {
        return channelRegistry.find(channelName)
                .map(this::ensureInfrastructureForChannel)
                .orElse(false);
    }

    /**
     * Crea la infraestructura para un canal: exchange + cola + binding específico
     */
    private boolean ensureInfrastructureForChannel(ChannelRegistryProperties.Channel channel) {
        try {
            String exchangeName = channel.getExchange();
            String queueName = channel.getName(); // La cola tiene el mismo nombre que el canal
            String routingKey = channel.getRoutingKey();
            
            // Crear exchange, cola y binding específico
            ensureExchange(exchangeName);
            ensureQueue(queueName);
            ensureBinding(exchangeName, queueName, routingKey);
            
            log.info("Infrastructure created for channel: {} -> exchange: {}, queue: {}, routingKey: {}", 
                    channel.getName(), exchangeName, queueName, routingKey);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to ensure infrastructure for channel: {}", channel.getName(), e);
            return false;
        }
    }

    private void ensureExchange(String exchangeName) {
        if (!createdExchanges.containsKey(exchangeName)) {
            log.info("Creating exchange: {}", exchangeName);
            TopicExchange exchange = new TopicExchange(exchangeName, true, false);
            rabbitAdmin.declareExchange(exchange);
            createdExchanges.put(exchangeName, true);
            log.info("Exchange created successfully: {}", exchangeName);
        } else {
            log.debug("Exchange already exists: {}", exchangeName);
        }
    }

    private void ensureQueue(String queueName) {
        if (!createdQueues.containsKey(queueName)) {
            log.info("Creating queue: {}", queueName);
            Queue queue = QueueBuilder.durable(queueName).build();
            rabbitAdmin.declareQueue(queue);
            createdQueues.put(queueName, true);
            log.info("Queue created successfully: {}", queueName);
        } else {
            log.debug("Queue already exists: {}", queueName);
        }
    }

    private void ensureBinding(String exchangeName, String queueName, String routingKey) {
        String bindingKey = exchangeName + ":" + queueName + ":" + routingKey;
        if (!createdBindings.containsKey(bindingKey)) {
            log.info("Creating binding: {} -> {} with routing key: {}", exchangeName, queueName, routingKey);
            Queue queue = new Queue(queueName, true);
            TopicExchange exchange = new TopicExchange(exchangeName, true, false);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
            rabbitAdmin.declareBinding(binding);
            createdBindings.put(bindingKey, true);
            log.info("Binding created successfully: {} -> {} with routing key: {}", exchangeName, queueName, routingKey);
        } else {
            log.debug("Binding already exists: {} -> {} with routing key: {}", exchangeName, queueName, routingKey);
        }
    }

    public void initializeAllChannels() {
        log.info("Initializing RabbitMQ infrastructure for all configured channels...");
        
        var allChannels = channelRegistry.getAllChannels();
        if (allChannels.isEmpty()) {
            log.warn("No channels found in registry - infrastructure will be created on-demand");
            return;
        }
        
        int successCount = 0;
        for (var channel : allChannels.values()) {
            try {
                if (ensureInfrastructureForChannel(channel)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to initialize infrastructure for channel: {}", channel.getName(), e);
            }
        }
        
        log.info("RabbitMQ infrastructure initialization completed: {}/{} channels initialized successfully", 
                successCount, allChannels.size());
    }

    public boolean isInfrastructureReady(String channelName) {
        return channelRegistry.find(channelName)
                .map(channel -> {
                    String exchangeName = channel.getExchange();
                    String queueName = channel.getName();
                    String routingKey = channel.getRoutingKey();
                    String bindingKey = exchangeName + ":" + queueName + ":" + routingKey;
                    
                    return createdExchanges.containsKey(exchangeName) && 
                           createdQueues.containsKey(queueName) &&
                           createdBindings.containsKey(bindingKey);
                })
                .orElse(false);
    }
}

