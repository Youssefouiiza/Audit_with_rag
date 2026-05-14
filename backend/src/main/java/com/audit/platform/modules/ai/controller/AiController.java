package com.audit.platform.modules.ai.controller;

import com.audit.platform.messaging.producer.AiAnalysisProducer;
import com.audit.platform.modules.ai.dto.AiResultResponse;
import com.audit.platform.modules.ai.service.AiService;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.repository.AuditRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI Analysis — trigger via RabbitMQ, fetch persisted results")
public class AiController {

    private final AiService             aiService;
    private final AiAnalysisProducer    aiAnalysisProducer;
    private final AuditRepository       auditRepository;

    /**
     * Returns the persisted AI result for the given audit.
     * Available to ADMIN, MANAGER, and AUDITOR.
     */
    @GetMapping("/result/{auditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "Get the AI analysis result for a specific audit")
    public ResponseEntity<AiResultResponse> getResult(@PathVariable UUID auditId) {
        return ResponseEntity.ok(aiService.getByAuditId(auditId));
    }

    /**
     * Triggers an AI analysis by publishing a message to RabbitMQ.
     * The Consumer picks it up asynchronously, calls the Python RAG service,
     * and persists the result — the HTTP thread is freed immediately (HTTP 202).
     *
     * Only AUDITOR role can trigger — Admins and Managers only read results.
     */
    @PostMapping("/analyze/{auditId}")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Trigger AI analysis asynchronously via RabbitMQ (Auditor only)")
    public ResponseEntity<Void> triggerAnalysis(
            @PathVariable UUID auditId,
            @AuthenticationPrincipal UserDetails principal) {

        // Load minimal audit info for the message payload (avoids loading inside consumer)
        Audit audit = auditRepository.findById(auditId).orElse(null);
        String title       = audit != null ? audit.getTitle()       : "";
        String description = audit != null ? audit.getDescription() : "";

        // Publish to RabbitMQ — returns immediately
        aiAnalysisProducer.publishAnalysisRequest(
                auditId,
                title,
                description,
                principal != null ? principal.getUsername() : "system"
        );

        return ResponseEntity.accepted().build(); // HTTP 202 — processing started asynchronously
    }
}
