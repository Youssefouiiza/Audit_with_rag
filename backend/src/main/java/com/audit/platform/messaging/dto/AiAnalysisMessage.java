package com.audit.platform.messaging.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message payload published to the audit.ai.analysis queue.
 * Carries everything the Consumer needs to call the Python RAG service
 * without any DB access.
 */
public record AiAnalysisMessage(
        UUID   auditId,
        String auditTitle,
        String auditDescription,
        String requestedByUsername
) implements Serializable {}
