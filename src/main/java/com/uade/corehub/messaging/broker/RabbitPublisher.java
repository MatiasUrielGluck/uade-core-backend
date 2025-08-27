package com.uade.corehub.messaging.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	public void publish(String exchange, String routingKey, Object payload) {
		rabbitTemplate.convertAndSend(exchange, routingKey, payload);
	}
}
