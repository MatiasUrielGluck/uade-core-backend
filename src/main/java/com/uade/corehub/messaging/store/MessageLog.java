package com.uade.corehub.messaging.store;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "message_log",
				indexes = {
								@Index(name="ix_message_log_channel", columnList = "channel"),
								@Index(name="ix_message_log_status", columnList = "status"),
								@Index(name="ix_message_log_created_at", columnList = "created_at DESC")
				},
				uniqueConstraints = @UniqueConstraint(name="ux_message_log_message_id", columnNames = "message_id")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageLog {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="message_id", nullable=false, length=64)
	private String messageId;

	@Column(nullable=false, length=120)
	private String channel;

	@Column(name="routing_key", nullable=false, length=120)
	private String routingKey;

	@Column(nullable=false, length=24)
	private String status; // PUBLISHED/FAILED/DLQ

	@Column(nullable=false)
	private int attempts;

	@Column(name="error_code", length=64)
	private String errorCode;

	@Column(name="error_message")
	private String errorMessage;

	@Column(name="correlation_id", length=64)
	private String correlationId;

	@Column(name="produced_at", nullable=false)
	private OffsetDateTime producedAt;

	@Column(name="published_at")
	private OffsetDateTime publishedAt;

	@Column(name="created_at", nullable=false)
	private OffsetDateTime createdAt;
}
