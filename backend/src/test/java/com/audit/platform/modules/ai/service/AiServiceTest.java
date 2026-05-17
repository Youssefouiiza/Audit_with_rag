package com.audit.platform.modules.ai.service;

import com.audit.platform.config.AppProperties;
import com.audit.platform.modules.ai.domain.AiResult;
import com.audit.platform.modules.ai.domain.RiskLevel;
import com.audit.platform.modules.ai.dto.AiResultResponse;
import com.audit.platform.modules.ai.repository.AiResultRepository;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.repository.AuditRepository;
import com.audit.platform.modules.document.repository.DocumentRepository;
import com.audit.platform.modules.document.service.MinioStorageService;
import com.audit.platform.modules.form.domain.AuditForm;
import com.audit.platform.modules.form.repository.AuditFormRepository;
import com.audit.platform.modules.notification.service.NotificationService;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.shared.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService Unit Tests")
class AiServiceTest {

    @Mock private AiResultRepository aiResultRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private AuditFormRepository formRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private MinioStorageService storageService;
    @Mock private NotificationService notificationService;
    @Mock private AppProperties appProperties;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AiService aiService;

    private User auditorUser, clientUser;
    private Audit audit;
    private UUID auditId;

    @BeforeEach
    void setUp() {
        clientUser = User.builder().id(UUID.randomUUID()).email("c@t.com").fullName("Client").role(UserRole.CLIENT).build();
        auditorUser = User.builder().id(UUID.randomUUID()).email("a@t.com").fullName("Auditor").role(UserRole.AUDITOR).build();
        auditId = UUID.randomUUID();
        audit = Audit.builder().id(auditId).title("Test").description("Desc").client(clientUser)
                .auditor(auditorUser).status(AuditStatus.IN_PROGRESS).build();
    }

    private void mockAiProps() {
        AppProperties.Ai ai = new AppProperties.Ai();
        // Leave API keys null so both callGroq and callMistral throw "not configured"
        lenient().when(appProperties.getAi()).thenReturn(ai);
    }

    // --- getByAuditId ---

    @Test
    @DisplayName("getByAuditId returns response with anomalies and recommendations")
    void getByAuditId_Success() {
        AiResult result = AiResult.builder().id(UUID.randomUUID()).audit(audit).modelUsed("groq/llama")
                .summary("Test summary").riskScore(75).riskLevel(RiskLevel.HIGH)
                .anomalies("[{\"titre\":\"A1\",\"description\":\"D1\",\"severite\":\"HIGH\",\"categorie\":\"FRAUDE\"}]")
                .recommendations("[{\"action\":\"Fix\",\"priorite\":\"IMMEDIATE\",\"responsable\":\"Auditeur\"}]")
                .processingTimeMs(5000L).createdAt(Instant.now()).build();
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.of(result));

        AiResultResponse r = aiService.getByAuditId(auditId);
        assertNotNull(r);
        assertEquals("Test summary", r.getSummary());
        assertEquals(75, r.getRiskScore());
        assertEquals(RiskLevel.HIGH, r.getRiskLevel());
        assertEquals(1, r.getAnomalies().size());
    }

    @Test
    @DisplayName("getByAuditId throws when not found")
    void getByAuditId_NotFound() {
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> aiService.getByAuditId(auditId));
    }

    @Test
    @DisplayName("getByAuditId handles empty anomalies JSON")
    void getByAuditId_EmptyAnomalies() {
        AiResult result = AiResult.builder().id(UUID.randomUUID()).audit(audit).modelUsed("m")
                .summary("s").riskScore(0).riskLevel(RiskLevel.LOW)
                .anomalies("[]").recommendations("[]")
                .processingTimeMs(100L).createdAt(Instant.now()).build();
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.of(result));

        AiResultResponse r = aiService.getByAuditId(auditId);
        assertTrue(r.getAnomalies().isEmpty());
    }

    @Test
    @DisplayName("getByAuditId handles null anomalies")
    void getByAuditId_NullAnomalies() {
        AiResult result = AiResult.builder().id(UUID.randomUUID()).audit(audit).modelUsed("m")
                .summary("s").riskScore(0).riskLevel(RiskLevel.LOW)
                .anomalies(null).recommendations(null)
                .processingTimeMs(100L).createdAt(Instant.now()).build();
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.of(result));

        AiResultResponse r = aiService.getByAuditId(auditId);
        assertTrue(r.getAnomalies().isEmpty());
    }

    @Test
    @DisplayName("getByAuditId handles malformed anomalies JSON")
    void getByAuditId_MalformedJson() {
        AiResult result = AiResult.builder().id(UUID.randomUUID()).audit(audit).modelUsed("m")
                .summary("s").riskScore(0).riskLevel(RiskLevel.LOW)
                .anomalies("not valid json").recommendations("also bad")
                .processingTimeMs(100L).createdAt(Instant.now()).build();
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.of(result));

        AiResultResponse r = aiService.getByAuditId(auditId);
        // Should gracefully return empty lists
        assertTrue(r.getAnomalies().isEmpty());
    }

    // --- analyzeAuditAsync (demo/simulation path) ---

    @Test
    @DisplayName("analyzeAuditAsync returns early when audit not found")
    void analyzeAsync_AuditNotFound() {
        mockAiProps();
        when(auditRepository.findById(auditId)).thenReturn(Optional.empty());
        CompletableFuture<Void> f = aiService.analyzeAuditAsync(auditId);
        assertNotNull(f);
        verify(aiResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("analyzeAuditAsync falls back to demo simulation without API keys")
    void analyzeAsync_DemoSimulation_NoDocsNoForm() {
        mockAiProps();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(documentRepository.findByAuditId(auditId)).thenReturn(List.of());
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(aiResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        aiService.analyzeAuditAsync(auditId);

        verify(aiResultRepository).save(argThat(r -> {
            AiResult ai = (AiResult) r;
            return ai.getModelUsed().contains("demo") && ai.getRiskScore() == 0;
        }));
    }

    @Test
    @DisplayName("analyzeAuditAsync demo simulation with form but no docs")
    void analyzeAsync_DemoSimulation_FormNoDocs() {
        mockAiProps();
        AuditForm form = AuditForm.builder().companyName("Co").legalForm("SARL")
                .revenue(BigDecimal.valueOf(500000)).build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.of(form));
        when(documentRepository.findByAuditId(auditId)).thenReturn(List.of());
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(aiResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        aiService.analyzeAuditAsync(auditId);

        verify(aiResultRepository).save(argThat(r -> {
            AiResult ai = (AiResult) r;
            return ai.getRiskScore() == 30 && ai.getRiskLevel() == RiskLevel.MEDIUM;
        }));
    }

    @Test
    @DisplayName("analyzeAuditAsync notifies auditor on completion")
    void analyzeAsync_NotifiesAuditor() {
        mockAiProps();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(documentRepository.findByAuditId(auditId)).thenReturn(List.of());
        when(aiResultRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(aiResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        aiService.analyzeAuditAsync(auditId);

        verify(notificationService).push(eq(auditorUser), any(), any(), any(), any(), any());
    }
}
