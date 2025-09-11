package com.uade.corehub.messaging.infrastructure;

import com.uade.corehub.config.RabbitMQInfrastructureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio para inicializar la infraestructura de RabbitMQ desde el archivo de configuración
 * Crea exchanges, colas y bindings definidos en rabbitmq-infrastructure.yaml
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQInfrastructureInitializer implements CommandLineRunner {

    private final RabbitAdmin rabbitAdmin;
    private final RabbitMQInfrastructureProperties infrastructureProperties;

    private final ConcurrentMap<String, Boolean> createdExchanges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> createdQueues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> createdBindings = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing RabbitMQ infrastructure from configuration...");
        
        if (infrastructureProperties == null || 
            infrastructureProperties.getExchanges() == null || 
            infrastructureProperties.getQueues() == null || 
            infrastructureProperties.getBindings() == null) {
            log.warn("RabbitMQ infrastructure properties not configured. Skipping initialization.");
            return;
        }

        try {
            // 1. Crear exchanges
            createExchanges();
            
            // 2. Crear colas
            createQueues();
            
            // 3. Crear bindings
            createBindings();
            
            log.info("RabbitMQ infrastructure initialization completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize RabbitMQ infrastructure", e);
            throw e;
        }
    }

    private void createExchanges() {
        log.info("Creating {} exchanges...", infrastructureProperties.getExchanges().size());
        
        for (RabbitMQInfrastructureProperties.Exchange exchangeConfig : infrastructureProperties.getExchanges()) {
            try {
                if (createdExchanges.containsKey(exchangeConfig.getName())) {
                    log.debug("Exchange already created: {}", exchangeConfig.getName());
                    continue;
                }

                Exchange exchange;
                switch (exchangeConfig.getType().toLowerCase()) {
                    case "topic":
                        exchange = new TopicExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete());
                        break;
                    case "direct":
                        exchange = new DirectExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete());
                        break;
                    case "fanout":
                        exchange = new FanoutExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete());
                        break;
                    default:
                        log.warn("Unknown exchange type: {}. Using TopicExchange as default.", exchangeConfig.getType());
                        exchange = new TopicExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete());
                }

                rabbitAdmin.declareExchange(exchange);
                createdExchanges.put(exchangeConfig.getName(), true);
                
                log.info("Created exchange: {} (type: {}, durable: {}, autoDelete: {})", 
                        exchangeConfig.getName(), exchangeConfig.getType(), 
                        exchangeConfig.isDurable(), exchangeConfig.isAutoDelete());

            } catch (Exception e) {
                log.error("Failed to create exchange: {}", exchangeConfig.getName(), e);
                createdExchanges.put(exchangeConfig.getName(), false);
            }
        }
    }

    private void createQueues() {
        log.info("Creating {} queues...", infrastructureProperties.getQueues().size());
        
        for (RabbitMQInfrastructureProperties.Queue queueConfig : infrastructureProperties.getQueues()) {
            try {
                if (createdQueues.containsKey(queueConfig.getName())) {
                    log.debug("Queue already created: {}", queueConfig.getName());
                    continue;
                }

                Queue queue;
                if (queueConfig.isAutoDelete()) {
                    queue = QueueBuilder.nonDurable(queueConfig.getName()).autoDelete().build();
                } else {
                    queue = QueueBuilder.durable(queueConfig.getName()).build();
                }

                rabbitAdmin.declareQueue(queue);
                createdQueues.put(queueConfig.getName(), true);
                
                log.info("Created queue: {} (durable: {}, autoDelete: {})", 
                        queueConfig.getName(), queueConfig.isDurable(), queueConfig.isAutoDelete());

            } catch (Exception e) {
                log.error("Failed to create queue: {}", queueConfig.getName(), e);
                createdQueues.put(queueConfig.getName(), false);
            }
        }
    }

    private void createBindings() {
        log.info("Creating {} bindings...", infrastructureProperties.getBindings().size());
        
        for (RabbitMQInfrastructureProperties.Binding bindingConfig : infrastructureProperties.getBindings()) {
            try {
                String bindingKey = bindingConfig.getExchange() + ":" + bindingConfig.getQueue() + ":" + bindingConfig.getRoutingKey();
                
                if (createdBindings.containsKey(bindingKey)) {
                    log.debug("Binding already created: {} -> {} with routing key: {}", 
                            bindingConfig.getExchange(), bindingConfig.getQueue(), bindingConfig.getRoutingKey());
                    continue;
                }

                // Verificar que el exchange y la cola existan
                if (!createdExchanges.getOrDefault(bindingConfig.getExchange(), false)) {
                    log.warn("Exchange {} does not exist. Skipping binding.", bindingConfig.getExchange());
                    continue;
                }

                if (!createdQueues.getOrDefault(bindingConfig.getQueue(), false)) {
                    log.warn("Queue {} does not exist. Skipping binding.", bindingConfig.getQueue());
                    continue;
                }

                // Crear el binding
                Queue queue = new Queue(bindingConfig.getQueue(), true);
                TopicExchange exchange = new TopicExchange(bindingConfig.getExchange(), true, false);
                Binding binding = BindingBuilder.bind(queue).to(exchange).with(bindingConfig.getRoutingKey());

                rabbitAdmin.declareBinding(binding);
                createdBindings.put(bindingKey, true);
                
                log.info("Created binding: {} -> {} with routing key: {}", 
                        bindingConfig.getExchange(), bindingConfig.getQueue(), bindingConfig.getRoutingKey());

            } catch (Exception e) {
                log.error("Failed to create binding: {} -> {} with routing key: {}", 
                        bindingConfig.getExchange(), bindingConfig.getQueue(), bindingConfig.getRoutingKey(), e);
                String bindingKey = bindingConfig.getExchange() + ":" + bindingConfig.getQueue() + ":" + bindingConfig.getRoutingKey();
                createdBindings.put(bindingKey, false);
            }
        }
    }

    /**
     * Obtiene el estado de la inicialización
     */
    public InfrastructureStatus getInfrastructureStatus() {
        int totalExchanges = infrastructureProperties.getExchanges().size();
        int totalQueues = infrastructureProperties.getQueues().size();
        int totalBindings = infrastructureProperties.getBindings().size();
        
        int createdExchangesCount = (int) createdExchanges.values().stream().filter(Boolean::booleanValue).count();
        int createdQueuesCount = (int) createdQueues.values().stream().filter(Boolean::booleanValue).count();
        int createdBindingsCount = (int) createdBindings.values().stream().filter(Boolean::booleanValue).count();

        return new InfrastructureStatus(
                totalExchanges, createdExchangesCount,
                totalQueues, createdQueuesCount,
                totalBindings, createdBindingsCount
        );
    }

    public record InfrastructureStatus(
            int totalExchanges, int createdExchanges,
            int totalQueues, int createdQueues,
            int totalBindings, int createdBindings
    ) {
        public boolean isComplete() {
            return createdExchanges == totalExchanges && 
                   createdQueues == totalQueues && 
                   createdBindings == totalBindings;
        }
    }
}
