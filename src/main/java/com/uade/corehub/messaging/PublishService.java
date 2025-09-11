package com.uade.corehub.messaging;

import com.uade.corehub.channels.ChannelRegistry;
import com.uade.corehub.channels.ChannelRegistryProperties;
import com.uade.corehub.messaging.broker.RabbitPublisher;
import com.uade.corehub.messaging.dto.MessageEnvelope;
import com.uade.corehub.messaging.infrastructure.RabbitMQInfrastructureService;
import com.uade.corehub.messaging.store.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishService {

	private final ChannelRegistry channelRegistry;
	private final RabbitPublisher rabbitPublisher;
	private final MessageLogRepository messageLogRepo;
	private final PayloadStoreRepository payloadRepo;
	private final RabbitMQInfrastructureService infrastructureService;

	@Transactional
	public void publish(MessageEnvelope env, String correlationId) {

		// 1) Canal válido o crear dinámicamente
		var ch = channelRegistry.find(env.destination().channel())
				.orElseGet(() -> createChannelDynamically(env.destination().channel()));

		// 2) Asegurar infraestructura
		infrastructureService.ensureInfrastructureForChannel(env.destination().channel());

		// 3) Idempotencia
		var existing = messageLogRepo.findByMessageId(env.messageId());
		if (existing.isPresent()) {
			return;
		}

		// 4) Persistencia inicial
		var now = OffsetDateTime.now();
		var messageLog = MessageLog.builder()
						.messageId(env.messageId())
						.channel(ch.getName())
						.routingKey(ch.getRoutingKey())
						.status("PUBLISHING")
						.attempts(0)
						.correlationId(correlationId)
						.producedAt(env.timestamp())
						.createdAt(now)
						.build();
		messageLog = messageLogRepo.save(messageLog);

		@SuppressWarnings("unchecked")
		Map<String, Object> payload = (env.payload() instanceof java.util.Map<?,?> m) ? 
			(java.util.Map<String, Object>) m : 
			java.util.Map.of("value", env.payload());

		payloadRepo.save(PayloadStore.builder()
						.messageId(env.messageId())
						.payload(payload)
						.schemaVer(null)
						.createdAt(now)
						.build());

		// 5) Publicar usando el routing key específico del canal
		try {
			log.info("Publishing message - Exchange: '{}', RoutingKey: '{}', MessageId: '{}'", 
					ch.getExchange(), ch.getRoutingKey(), env.messageId());
			
			// Publicar el envelope completo (como estaba originalmente)
			rabbitPublisher.publish(ch.getExchange(), ch.getRoutingKey(), env);
			
			messageLog.setStatus("PUBLISHED");
			messageLog.setAttempts(messageLog.getAttempts() + 1);
			messageLog.setPublishedAt(OffsetDateTime.now());
			messageLogRepo.save(messageLog);
			
			log.info("Message published successfully - MessageId: '{}'", env.messageId());
		} catch (Exception ex) {
			log.error("Failed to publish message - MessageId: '{}', Error: {}", env.messageId(), ex.getMessage(), ex);
			messageLog.setStatus("FAILED");
			messageLog.setAttempts(messageLog.getAttempts() + 1);
			messageLog.setErrorMessage(ex.getMessage());
			messageLogRepo.save(messageLog);
			throw ex;
		}
	}

	/**
	 * Crea un canal dinámicamente basándose en el nombre del canal
	 */
	private ChannelRegistryProperties.Channel createChannelDynamically(String channelName) {
		try {
			String[] parts = channelName.split("\\.");
			if (parts.length < 2) {
				throw new IllegalArgumentException("Channel name must follow pattern: squad.topic.event (e.g., payments.order.created)");
			}

			String squad = parts[0];
			String exchange = "corehub.x." + squad;
			String routingKey = channelName;

			var channel = new ChannelRegistryProperties.Channel();
			channel.setName(channelName);
			channel.setExchange(exchange);
			channel.setRoutingKey(routingKey);

			// Agregar al registry
			boolean added = channelRegistry.addChannel(channel);
			if (!added) {
				// Si no se pudo agregar, intentar obtener el existente
				return channelRegistry.find(channelName)
						.orElseThrow(() -> new IllegalArgumentException("Failed to create or find channel: " + channelName));
			}

			log.info("Created channel dynamically: {} -> exchange: {}, routingKey: {}", 
					channelName, exchange, routingKey);

			return channel;

		} catch (Exception e) {
			log.error("Failed to create channel dynamically: {}", channelName, e);
			throw new IllegalArgumentException("Cannot create channel dynamically: " + channelName + " - " + e.getMessage());
		}
	}
}
