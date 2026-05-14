package com.audit.platform.messaging.producer;

import com.audit.platform.messaging.config.RabbitMQConfig;
import com.audit.platform.messaging.dto.AiAnalysisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Producer — publishes AI analysis requests to RabbitMQ.
 *
 * The controller calls this instead of blocking on the Python RAG service,
 * freeing the HTTP thread immediately and returning HTTP 202 Accepted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes an {@link AiAnalysisMessage} to the Topic Exchange so that
     * the {@link com.audit.platform.messaging.consumer.AiAnalysisConsumer}
     * picks it up and calls the Python RAG service asynchronously.
     *
     * @param auditId          target audit UUID
     * @param auditTitle       title carried in the message (avoids extra DB query in consumer)
     * @param auditDescription description carried in the message
     * @param requestedBy      username of the Auditor who triggered the analysis
     */
    public void publishAnalysisRequest(UUID auditId,
                                       String auditTitle,
                                       String auditDescription,
                                       String requestedBy) {
        AiAnalysisMessage message = new AiAnalysisMessage(
                auditId, auditTitle, auditDescription, requestedBy
        );

        log.info("[PRODUCER] Publishing AI analysis request for auditId={} by={}",
                auditId, requestedBy);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.AI_ROUTING_KEY,
                message
        );

        log.debug("[PRODUCER] Message published successfully → queue={}", RabbitMQConfig.AI_QUEUE);
    }
}
