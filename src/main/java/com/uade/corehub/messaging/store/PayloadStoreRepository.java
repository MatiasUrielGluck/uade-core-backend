package com.uade.corehub.messaging.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayloadStoreRepository extends JpaRepository<PayloadStore, Long> {
	Optional<PayloadStore> findByMessageId(String messageId);
}
