package com.audit.platform.messaging.producer;

import com.audit.platform.messaging.config.RabbitMQConfig;
import com.audit.platform.messaging.dto.AiAnalysisMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAnalysisProducer Unit Tests")
class AiAnalysisProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AiAnalysisProducer producer;

    @Test
    @DisplayName("publishAnalysisRequest sends message with correct parameters")
    void publishAnalysisRequest_CorrectParams() {
        UUID auditId = UUID.randomUUID();

        producer.publishAnalysisRequest(auditId, "Test Audit", "Test desc", "auditor@test.com");

        ArgumentCaptor<AiAnalysisMessage> captor = ArgumentCaptor.forClass(AiAnalysisMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.AI_ROUTING_KEY),
                captor.capture()
        );

        AiAnalysisMessage msg = captor.getValue();
        assertEquals(auditId, msg.auditId());
        assertEquals("Test Audit", msg.auditTitle());
        assertEquals("Test desc", msg.auditDescription());
        assertEquals("auditor@test.com", msg.requestedByUsername());
    }

    @Test
    @DisplayName("publishAnalysisRequest handles null description")
    void publishAnalysisRequest_NullDescription() {
        UUID auditId = UUID.randomUUID();

        producer.publishAnalysisRequest(auditId, "Title", null, "user@e.com");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.AI_ROUTING_KEY),
                any(AiAnalysisMessage.class)
        );
    }
}
