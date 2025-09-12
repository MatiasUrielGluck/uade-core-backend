package com.uade.corehub.messaging.dispatcher;

import com.uade.corehub.messaging.SubscriptionService;
import com.uade.corehub.messaging.dto.MessageEnvelope;
import com.uade.corehub.messaging.store.Subscription;
import com.uade.corehub.channels.ChannelRegistry;
import com.uade.corehub.channels.ChannelRegistryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.uade.corehub.messaging.store.SubscriptionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcherService {

    private final SubscriptionService subscriptionService;
    private final RestTemplate webhookRestTemplate;
    private final SubscriptionRepository subscriptionRepository;
    private final ChannelRegistry channelRegistry;

    public void dispatch(MessageEnvelope envelope, String correlationId) {
        String channelName = envelope.destination().channel();
        String eventName = envelope.destination().eventName();

        String topic = channelRegistry.find(channelName)
                .map(ChannelRegistryProperties.Channel::getRoutingKey)
                .orElse(null);
        if (topic == null) {
            log.warn("Channel '{}' not found in registry; skipping dispatch for messageId='{}'", channelName, envelope.messageId());
            return;
        }

        List<Subscription> targets = subscriptionService.findMatchingSubscriptions(topic, eventName);
        if (targets.isEmpty()) {
            log.debug("No subscriptions matched for topic='{}', event='{}'", topic, eventName);
            return;
        }

        log.info("Dispatching messageId='{}' to {} webhook(s)", envelope.messageId(), targets.size());

        for (Subscription sub : targets) {
            sendToWebhook(sub, envelope, correlationId);
        }
    }

    private void sendToWebhook(Subscription subscription, MessageEnvelope envelope, String correlationId) {
        String url = subscription.getWebhookUrl();

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Correlation-Id", correlationId == null ? "" : correlationId);
            headers.add("X-Subscription-Id", subscription.getId());

            org.springframework.http.HttpEntity<MessageEnvelope> entity = new org.springframework.http.HttpEntity<>(envelope, headers);

            // Intento simple + reintentos básicos
            int maxRetries = 3;
            int attempt = 0;
            while (true) {
                try {
                    attempt++;
                    webhookRestTemplate.postForEntity(url, entity, Void.class);
                    updateSuccess(subscription);
                    break;
                } catch (Exception ex) {
                    if (attempt >= maxRetries) {
                        updateFailure(subscription, ex, envelope, url);
                        break;
                    }
                    log.warn("Retrying webhook {} attempt={}", url, attempt);
                    try { Thread.sleep(300L * attempt); } catch (InterruptedException ignored) {}
                }
            }

        } catch (Exception ex) {
            updateFailure(subscription, ex, envelope, url);
        }
    }

    @Transactional
    protected void updateSuccess(Subscription subscription) {
        subscription.setFailedAttempts(0);
        subscription.setLastError(null);
        subscription.setLastSuccessfulDelivery(OffsetDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Transactional
    protected void updateFailure(Subscription subscription, Throwable ex, MessageEnvelope envelope, String url) {
        subscription.setFailedAttempts(subscription.getFailedAttempts() + 1);
        subscription.setLastError(ex.getMessage());
        subscriptionRepository.save(subscription);
        log.error("Webhook delivery failed url={} subId={} msgId={} error={}", url, subscription.getId(), envelope.messageId(), ex.toString());
    }
}


