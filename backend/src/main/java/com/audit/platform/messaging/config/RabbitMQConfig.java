package com.audit.platform.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Audit Platform.
 *
 * Exchange : audit.exchange  (Topic)
 * Queues   :
 *   - audit.ai.analysis   → triggered when an Auditor launches AI analysis
 *   - audit.notifications → triggered for user notification fanout
 * Dead-Letter Queue : audit.ai.analysis.dlq  → failed analysis jobs land here
 */
@Configuration
public class RabbitMQConfig {

    /* ── Constants ── */
    public static final String EXCHANGE           = "audit.exchange";
    public static final String AI_QUEUE           = "audit.ai.analysis";
    public static final String AI_ROUTING_KEY     = "audit.ai.analyze";
    public static final String NOTIFY_QUEUE       = "audit.notifications";
    public static final String NOTIFY_ROUTING_KEY = "audit.notify";
    public static final String DLQ_QUEUE          = "audit.ai.analysis.dlq";
    public static final String DLQ_EXCHANGE       = "audit.dlx";

    /* ── Exchange ── */
    @Bean
    public TopicExchange auditExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLQ_EXCHANGE).durable(true).build();
    }

    /* ── Queues ── */
    @Bean
    public Queue aiAnalysisQueue() {
        return QueueBuilder.durable(AI_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
                .withArgument("x-message-ttl", 300_000) // 5 min TTL
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    /* ── Bindings ── */
    @Bean
    public Binding aiQueueBinding(Queue aiAnalysisQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(aiAnalysisQueue).to(auditExchange).with(AI_ROUTING_KEY);
    }

    @Bean
    public Binding notifyQueueBinding(Queue notificationQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(notificationQueue).to(auditExchange).with(NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_QUEUE);
    }

    /* ── Jackson serialization for messages ── */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
