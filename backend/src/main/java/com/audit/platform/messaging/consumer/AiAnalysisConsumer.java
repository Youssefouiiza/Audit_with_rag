package com.audit.platform.messaging.consumer;

import com.audit.platform.messaging.config.RabbitMQConfig;
import com.audit.platform.messaging.dto.AiAnalysisMessage;
import com.audit.platform.modules.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumer — listens on the audit.ai.analysis queue.
 *
 * Executes the heavy Python RAG call asynchronously on a separate thread pool
 * managed by Spring AMQP's listener container.  On failure, the message is
 * automatically sent to the Dead-Letter Queue (audit.ai.analysis.dlq) after
 * exhausting retries, so no analysis request is lost.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisConsumer {

    private final AiService aiService;

    /**
     * Triggered automatically when a message arrives in {@value RabbitMQConfig#AI_QUEUE}.
     *
     * @param message deserialized from JSON by Jackson2JsonMessageConverter
     */
    @RabbitListener(queues = RabbitMQConfig.AI_QUEUE, concurrency = "2-5")
    public void handleAnalysisRequest(AiAnalysisMessage message) {
        log.info("[CONSUMER] Received AI analysis request: auditId={} requestedBy={}",
                message.auditId(), message.requestedByUsername());
        try {
            // Delegate to the existing AiService — it calls the Python RAG WebClient
            aiService.analyzeAuditAsync(message.auditId());
            log.info("[CONSUMER] AI analysis triggered successfully for auditId={}", message.auditId());
        } catch (Exception ex) {
            log.error("[CONSUMER] Analysis failed for auditId={} — will be sent to DLQ: {}",
                    message.auditId(), ex.getMessage(), ex);
            // Re-throw so Spring AMQP nacks the message and routes it to the DLQ
            throw new RuntimeException("AI analysis failed for auditId=" + message.auditId(), ex);
        }
    }
}
