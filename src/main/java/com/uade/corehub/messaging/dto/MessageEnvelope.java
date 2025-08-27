package com.uade.corehub.messaging.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.Map;

public record MessageEnvelope(
	@NotBlank String messageId,
	@NotNull OffsetDateTime timestamp,
	@NotBlank String source,
	@Valid @NotNull Destination destination,
	Map<String, String> metadata,
	@NotNull Object payload
) {
	public record Destination(
		@NotBlank String channel,
		@NotBlank String eventName
	) {}
}
