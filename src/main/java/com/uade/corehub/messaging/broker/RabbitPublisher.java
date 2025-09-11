package com.uade.corehub.messaging.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	public void publish(String exchange, String routingKey, Object payload) {
		log.info("Publishing message to exchange: '{}' with routing key: '{}'", exchange, routingKey);
		try {
			rabbitTemplate.convertAndSend(exchange, routingKey, payload);
			log.info("Message published successfully to exchange: '{}' with routing key: '{}'", exchange, routingKey);
		} catch (Exception e) {
			log.error("Failed to publish message to exchange: '{}' with routing key: '{}'", exchange, routingKey, e);
			throw e;
		}
	}
}
