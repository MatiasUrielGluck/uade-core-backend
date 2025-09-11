package com.uade.corehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "rabbitmq.infrastructure")
public class RabbitMQInfrastructureProperties {

    private List<Exchange> exchanges;
    private List<Queue> queues;
    private List<Binding> bindings;

    @Data
    public static class Exchange {
        private String name;
        private String type;
        private boolean durable;
        private boolean autoDelete;
        private String description;
    }

    @Data
    public static class Queue {
        private String name;
        private boolean durable;
        private boolean autoDelete;
        private String description;
    }

    @Data
    public static class Binding {
        private String exchange;
        private String queue;
        private String routingKey;
        private String description;
    }
}
