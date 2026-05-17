package com.audit.platform.messaging.consumer;

import com.audit.platform.messaging.dto.AiAnalysisMessage;
import com.audit.platform.modules.ai.service.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAnalysisConsumer Unit Tests")
class AiAnalysisConsumerTest {

    @Mock private AiService aiService;

    @InjectMocks private AiAnalysisConsumer consumer;

    @Test
    @DisplayName("handleAnalysisRequest delegates to AiService")
    void handleRequest_Success() {
        UUID auditId = UUID.randomUUID();
        AiAnalysisMessage msg = new AiAnalysisMessage(auditId, "Title", "Desc", "user@e.com");
        when(aiService.analyzeAuditAsync(auditId)).thenReturn(CompletableFuture.completedFuture(null));

        consumer.handleAnalysisRequest(msg);
        verify(aiService).analyzeAuditAsync(auditId);
    }

    @Test
    @DisplayName("handleAnalysisRequest re-throws on failure for DLQ routing")
    void handleRequest_Failure() {
        UUID auditId = UUID.randomUUID();
        AiAnalysisMessage msg = new AiAnalysisMessage(auditId, "T", "D", "u");
        when(aiService.analyzeAuditAsync(auditId)).thenThrow(new RuntimeException("AI down"));

        assertThrows(RuntimeException.class, () -> consumer.handleAnalysisRequest(msg));
    }
}
