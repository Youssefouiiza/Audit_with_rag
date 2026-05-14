package com.audit.platform.messaging;

import com.audit.platform.messaging.config.RabbitMQConfig;
import com.audit.platform.messaging.dto.AiAnalysisMessage;
import com.audit.platform.messaging.producer.AiAnalysisProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the RabbitMQ messaging pipeline.
 *
 * Uses Testcontainers to spin up a real RabbitMQ instance in Docker,
 * so no external setup is needed. The test verifies the full round-trip:
 *   Producer → RabbitMQ → Consumer
 */
@SpringBootTest
@Testcontainers
class RabbitMQMessagingIntegrationTest {

    // Spins up a real RabbitMQ container automatically for this test class
    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void overrideRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host",     rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port",     rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired
    private AiAnalysisProducer producer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Producer should publish AiAnalysisMessage to the correct queue")
    void shouldPublishMessageToQueue() {
        UUID testAuditId = UUID.randomUUID();

        // Act — publish a message
        producer.publishAnalysisRequest(
                testAuditId,
                "Test Audit Title",
                "Test description",
                "auditor@test.com"
        );

        // Assert — message arrives in the queue within 5 seconds
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            AiAnalysisMessage received = (AiAnalysisMessage) rabbitTemplate.receiveAndConvert(
                    RabbitMQConfig.AI_QUEUE
            );
            assertThat(received).isNotNull();
            assertThat(received.auditId()).isEqualTo(testAuditId);
            assertThat(received.auditTitle()).isEqualTo("Test Audit Title");
            assertThat(received.requestedByUsername()).isEqualTo("auditor@test.com");
        });
    }

    @Test
    @DisplayName("Queue audit.ai.analysis.dlq should exist (Dead Letter Queue)")
    void deadLetterQueueShouldBeConfigured() {
        // DLQ must exist and be reachable
        Object dlqProps = rabbitTemplate.execute(channel -> {
            try {
                return channel.queueDeclarePassive(RabbitMQConfig.DLQ_QUEUE);
            } catch (Exception e) {
                return null;
            }
        });
        assertThat(dlqProps).isNotNull();
    }
}
