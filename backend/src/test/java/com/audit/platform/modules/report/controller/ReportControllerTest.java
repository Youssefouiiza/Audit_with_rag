package com.audit.platform.modules.report.controller;

import com.audit.platform.modules.report.domain.ReportStatus;
import com.audit.platform.modules.report.dto.ReportResponse;
import com.audit.platform.modules.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportController Unit Tests")
class ReportControllerTest {

    @Mock private ReportService reportService;
    @InjectMocks private ReportController controller;

    @Test
    @DisplayName("getByAuditId returns report")
    void getByAuditId() {
        UUID id = UUID.randomUUID();
        ReportResponse resp = ReportResponse.builder().id(UUID.randomUUID()).auditId(id).status(ReportStatus.READY).build();
        when(reportService.getByAuditId(id)).thenReturn(resp);
        ResponseEntity<ReportResponse> r = controller.getByAuditId(id);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(id, r.getBody().getAuditId());
    }

    @Test
    @DisplayName("generate returns report")
    void generate() {
        UUID id = UUID.randomUUID();
        ReportResponse resp = ReportResponse.builder().id(UUID.randomUUID()).status(ReportStatus.READY).build();
        when(reportService.generateReport(id)).thenReturn(resp);
        assertEquals(HttpStatus.OK, controller.generate(id).getStatusCode());
    }

    @Test
    @DisplayName("submit extracts keys from body")
    void submit() {
        UUID id = UUID.randomUUID();
        ReportResponse resp = ReportResponse.builder().id(UUID.randomUUID()).status(ReportStatus.PENDING_REVIEW).build();
        when(reportService.submitReport(id, "fk", "fn")).thenReturn(resp);
        ResponseEntity<ReportResponse> r = controller.submit(id, Map.of("documentFileKey", "fk", "documentFileName", "fn"));
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(ReportStatus.PENDING_REVIEW, r.getBody().getStatus());
    }

    @Test
    @DisplayName("review passes decision and comment")
    void review() {
        UUID id = UUID.randomUUID();
        ReportResponse resp = ReportResponse.builder().id(UUID.randomUUID()).status(ReportStatus.APPROVED).build();
        when(reportService.reviewReport(id, "APPROVE", "Good")).thenReturn(resp);
        ResponseEntity<ReportResponse> r = controller.review(id, Map.of("decision", "APPROVE", "comment", "Good"));
        assertEquals(ReportStatus.APPROVED, r.getBody().getStatus());
    }

    @Test
    @DisplayName("review defaults decision to empty string")
    void reviewDefaultDecision() {
        UUID id = UUID.randomUUID();
        ReportResponse resp = ReportResponse.builder().id(UUID.randomUUID()).build();
        when(reportService.reviewReport(id, "", null)).thenReturn(resp);
        controller.review(id, Map.of());
        verify(reportService).reviewReport(id, "", null);
    }
}
