package com.uade.corehub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
		return tpl;
	}
}
