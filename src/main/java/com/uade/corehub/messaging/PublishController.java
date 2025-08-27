package com.uade.corehub.messaging;

import com.uade.corehub.messaging.dto.MessageEnvelope;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/publish")
public class PublishController {

	private final PublishService service;

	public PublishController(PublishService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Void> publish(@Valid @RequestBody MessageEnvelope envelope,
																			@RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
		service.publish(envelope, correlationId);
		return ResponseEntity.accepted().build();
	}
}
