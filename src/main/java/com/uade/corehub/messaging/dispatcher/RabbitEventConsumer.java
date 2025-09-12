package com.uade.corehub.messaging.dispatcher;

import com.uade.corehub.messaging.dto.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEventConsumer {

    private final WebhookDispatcherService dispatcherService;

    // Listener genérico: una cola por canal (el nombre de la cola = nombre del canal)
    // Se podrían agregar más @RabbitListener si hay múltiples colas
    @RabbitListener(queues = "#{channelRegistry.getAllChannels().keySet().toArray(new String[0])}")
    public void onMessage(MessageEnvelope envelope,
                          @Header(name = "X-Correlation-Id", required = false) String correlationId) {
        try {
            log.info("Received message for dispatch. messageId='{}' channel='{}'", envelope.messageId(), envelope.destination().channel());
            dispatcherService.dispatch(envelope, correlationId);
        } catch (Exception e) {
            log.error("Error processing message for dispatch msgId={} error={}", envelope.messageId(), e.toString(), e);
            throw e;
        }
    }
}


