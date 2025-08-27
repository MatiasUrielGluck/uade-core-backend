package com.uade.corehub.messaging.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
	Optional<MessageLog> findByMessageId(String messageId);
}
