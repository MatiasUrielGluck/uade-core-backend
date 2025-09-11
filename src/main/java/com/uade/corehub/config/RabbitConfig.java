package com.uade.corehub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.corehub.messaging.infrastructure.RabbitMQInfrastructureService;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	@Bean
	public MessageConverter jacksonMessageConverter(ObjectMapper mapper) {
		// Usa el ObjectMapper de Spring (respeta config global, módulos JavaTime, etc.)
		Jackson2JsonMessageConverter conv = new Jackson2JsonMessageConverter(mapper);
		conv.setCreateMessageIds(true);
		return conv;
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter jacksonMessageConverter) {
		RabbitTemplate tpl = new RabbitTemplate(cf);
		tpl.setMandatory(true);
		tpl.setMessageConverter(jacksonMessageConverter);
		tpl.setBeforePublishPostProcessors(m -> {
			m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			return m;
		});
		
		// Callback para mensajes no enrutados
		tpl.setReturnsCallback(returned -> {
			System.err.println("Message not routed: " + returned.getMessage());
			System.err.println("Reply code: " + returned.getReplyCode());
			System.err.println("Reply text: " + returned.getReplyText());
			System.err.println("Exchange: " + returned.getExchange());
			System.err.println("Routing key: " + returned.getRoutingKey());
		});
		
		return tpl;
	}

	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	/**
	 * Inicialización automática de la infraestructura de RabbitMQ
	 * Crea exchanges, colas y bindings específicos para cada canal
	 */
	@Bean
	public CommandLineRunner setupRabbitMQInfrastructure(RabbitMQInfrastructureService infrastructureService) {
		return args -> {
			infrastructureService.initializeAllChannels();
		};
	}

	/**
	 * Configuración de ejemplo para mostrar la arquitectura correcta
	 * En producción, esto se maneja dinámicamente por el ChannelRegistry
	 */
	/*
	@Bean
	TopicExchange paymentsExchange() {
		return new TopicExchange("corehub.x.payments", true, false);
	}

	@Bean
	Queue orderCreatedQueue() {
		return new Queue("payments.order.created", true);
	}

	@Bean
	Queue orderCanceledQueue() {
		return new Queue("payments.order.canceled", true);
	}

	@Bean
	Binding bindingOrderCreated(Queue orderCreatedQueue, TopicExchange paymentsExchange) {
		return BindingBuilder.bind(orderCreatedQueue).to(paymentsExchange).with("payments.order.created");
	}

	@Bean
	Binding bindingOrderCanceled(Queue orderCanceledQueue, TopicExchange paymentsExchange) {
		return BindingBuilder.bind(orderCanceledQueue).to(paymentsExchange).with("payments.order.canceled");
	}
	*/
}
