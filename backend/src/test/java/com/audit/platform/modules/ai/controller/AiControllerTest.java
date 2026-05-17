package com.audit.platform.modules.ai.controller;

import com.audit.platform.messaging.producer.AiAnalysisProducer;
import com.audit.platform.modules.ai.domain.RiskLevel;
import com.audit.platform.modules.ai.dto.AiResultResponse;
import com.audit.platform.modules.ai.service.AiService;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.repository.AuditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiController Unit Tests")
class AiControllerTest {

    @Mock private AiService aiService;
    @Mock private AiAnalysisProducer aiAnalysisProducer;
    @Mock private AuditRepository auditRepository;
    @InjectMocks private AiController controller;

    @Test
    @DisplayName("getResult returns AI result")
    void getResult() {
        UUID id = UUID.randomUUID();
        AiResultResponse resp = AiResultResponse.builder().auditId(id).riskLevel(RiskLevel.HIGH)
                .riskScore(75).summary("Summary").anomalies(List.of()).build();
        when(aiService.getByAuditId(id)).thenReturn(resp);
        ResponseEntity<AiResultResponse> r = controller.getResult(id);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(RiskLevel.HIGH, r.getBody().getRiskLevel());
    }

    @Test
    @DisplayName("triggerAnalysis returns 202 and publishes message")
    void triggerAnalysis_WithAudit() {
        UUID id = UUID.randomUUID();
        Audit audit = Audit.builder().id(id).title("Test").description("Desc").build();
        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("auditor@e.com");

        ResponseEntity<Void> r = controller.triggerAnalysis(id, principal);
        assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
        verify(aiAnalysisProducer).publishAnalysisRequest(id, "Test", "Desc", "auditor@e.com");
    }

    @Test
    @DisplayName("triggerAnalysis handles missing audit gracefully")
    void triggerAnalysis_NoAudit() {
        UUID id = UUID.randomUUID();
        when(auditRepository.findById(id)).thenReturn(Optional.empty());
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("user");

        ResponseEntity<Void> r = controller.triggerAnalysis(id, principal);
        assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
        verify(aiAnalysisProducer).publishAnalysisRequest(id, "", "", "user");
    }

    @Test
    @DisplayName("triggerAnalysis handles null principal")
    void triggerAnalysis_NullPrincipal() {
        UUID id = UUID.randomUUID();
        when(auditRepository.findById(id)).thenReturn(Optional.empty());
        ResponseEntity<Void> r = controller.triggerAnalysis(id, null);
        assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
        verify(aiAnalysisProducer).publishAnalysisRequest(id, "", "", "system");
    }
}
