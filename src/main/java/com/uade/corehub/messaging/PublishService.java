package com.uade.corehub.messaging;

import com.uade.corehub.channels.ChannelRegistry;
import com.uade.corehub.messaging.broker.RabbitPublisher;
import com.uade.corehub.messaging.dto.MessageEnvelope;
import com.uade.corehub.messaging.store.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PublishService {

	private final ChannelRegistry channelRegistry;
	private final RabbitPublisher rabbitPublisher;
	private final MessageLogRepository messageLogRepo;
	private final PayloadStoreRepository payloadRepo;

	@Transactional
	public void publish(MessageEnvelope env, String correlationId) {

		// 1) Canal válido
		var ch = channelRegistry.find(env.destination().channel())
						.orElseThrow(() -> new IllegalArgumentException("Unknown channel: " + env.destination().channel()));

		// 2) Idempotencia
		var existing = messageLogRepo.findByMessageId(env.messageId());
		if (existing.isPresent()) {
			// ya registrado (decidir re-publicar o no)
			return;
		}

		// 3) Persistencia inicial
		var now = OffsetDateTime.now();
		var log = MessageLog.builder()
						.messageId(env.messageId())
						.channel(ch.getName())
						.routingKey(ch.getRoutingKey())
						.status("PUBLISHING")
						.attempts(0)
						.correlationId(correlationId)
						.producedAt(env.timestamp())
						.createdAt(now)
						.build();
		log = messageLogRepo.save(log);

		payloadRepo.save(PayloadStore.builder()
						.messageId(env.messageId())
						.payload((env.payload() instanceof java.util.Map<?,?> m) ? (java.util.Map) m : java.util.Map.of("value", env.payload()))
						.schemaVer(null)
						.createdAt(now)
						.build());

		// 4) Publicar
		try {
			rabbitPublisher.publish(ch.getExchange(), ch.getRoutingKey(), env);
			log.setStatus("PUBLISHED");
			log.setAttempts(log.getAttempts() + 1);
			log.setPublishedAt(OffsetDateTime.now());
			messageLogRepo.save(log);
		} catch (Exception ex) {
			log.setStatus("FAILED");
			log.setAttempts(log.getAttempts() + 1);
			log.setErrorMessage(ex.getMessage());
			messageLogRepo.save(log);
			throw ex;
		}
	}
}
