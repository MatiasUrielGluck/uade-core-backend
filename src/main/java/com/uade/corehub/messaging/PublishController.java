package com.uade.corehub.messaging;

import com.uade.corehub.messaging.dto.MessageEnvelope;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/publish")
public class PublishController {

	private final PublishService service;

	public PublishController(PublishService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<?> publish(@Valid @RequestBody MessageEnvelope envelope,
																			@RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
		log.info("Received publish request - MessageId: '{}', Channel: '{}', CorrelationId: '{}'", 
				envelope.messageId(), envelope.destination().channel(), correlationId);
		
		try {
			service.publish(envelope, correlationId);
			log.info("Publish request completed - MessageId: '{}'", envelope.messageId());
			return ResponseEntity.accepted().build();
		} catch (IllegalArgumentException e) {
			log.warn("Publish request failed - MessageId: '{}', Error: {}", envelope.messageId(), e.getMessage());
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			log.error("Publish request failed - MessageId: '{}', Error: {}", envelope.messageId(), e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
