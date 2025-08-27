package com.uade.corehub.messaging.store;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name="payload_store",
				indexes = {
								@Index(name="ix_payload_store_message_id", columnList = "message_id"),
								@Index(name="ix_payload_store_created_at", columnList = "created_at DESC")
				})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayloadStore {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="message_id", nullable=false, length=64)
	private String messageId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name="payload", nullable=false, columnDefinition = "jsonb")
	private Map<String, Object> payload;

	@Column(name="schema_ver", length=32)
	private String schemaVer;

	@Column(name="created_at", nullable=false)
	private OffsetDateTime createdAt;
}
