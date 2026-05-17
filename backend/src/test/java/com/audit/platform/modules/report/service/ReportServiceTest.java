package com.audit.platform.modules.report.service;

import com.audit.platform.config.AppProperties;
import com.audit.platform.config.SecurityUserDetails;
import com.audit.platform.modules.ai.repository.AiResultRepository;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.repository.AuditRepository;
import com.audit.platform.modules.document.service.MinioStorageService;
import com.audit.platform.modules.form.repository.AuditFormRepository;
import com.audit.platform.modules.notification.service.NotificationService;
import com.audit.platform.modules.report.domain.Report;
import com.audit.platform.modules.report.domain.ReportStatus;
import com.audit.platform.modules.report.dto.ReportResponse;
import com.audit.platform.modules.report.repository.ReportRepository;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.repository.UserRepository;
import com.audit.platform.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private AuditFormRepository formRepository;
    @Mock private AiResultRepository aiResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioStorageService storageService;
    @Mock private NotificationService notificationService;
    @Mock private AppProperties appProperties;

    @InjectMocks private ReportService reportService;

    private User auditorUser, managerUser, clientUser;
    private Audit audit;
    private UUID auditId;

    @BeforeEach
    void setUp() {
        clientUser = User.builder().id(UUID.randomUUID()).email("c@t.com").fullName("Client").role(UserRole.CLIENT).build();
        auditorUser = User.builder().id(UUID.randomUUID()).email("a@t.com").fullName("Auditor").role(UserRole.AUDITOR).build();
        managerUser = User.builder().id(UUID.randomUUID()).email("m@t.com").fullName("Manager").role(UserRole.MANAGER).build();
        auditId = UUID.randomUUID();
        audit = Audit.builder().id(auditId).title("Test Audit").client(clientUser).auditor(auditorUser).status(AuditStatus.IN_PROGRESS).build();
    }

    private void mockSecurityContext(User user) {
        SecurityUserDetails details = new SecurityUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // --- submitReport ---

    @Test
    @DisplayName("submitReport creates new report with PENDING_REVIEW status")
    void submitReport_NewReport() {
        mockSecurityContext(auditorUser);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(auditorUser.getId())).thenReturn(Optional.of(auditorUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(userRepository.findByRole(UserRole.MANAGER)).thenReturn(List.of(managerUser));

        Report saved = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.PENDING_REVIEW).documentFileKey("doc-key").documentFileName("report.pdf").build();
        when(reportRepository.save(any())).thenReturn(saved);

        AppProperties.Minio minio = new AppProperties.Minio();
        lenient().when(appProperties.getMinio()).thenReturn(minio);

        ReportResponse r = reportService.submitReport(auditId, "doc-key", "report.pdf");
        assertNotNull(r);
        assertEquals(ReportStatus.PENDING_REVIEW, r.getStatus());
        verify(notificationService).push(eq(managerUser), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("submitReport updates existing report")
    void submitReport_UpdatesExisting() {
        mockSecurityContext(auditorUser);
        Report existing = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.REJECTED).build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(auditorUser.getId())).thenReturn(Optional.of(auditorUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.of(existing));
        when(userRepository.findByRole(UserRole.MANAGER)).thenReturn(List.of());
        when(reportRepository.save(any())).thenReturn(existing);
        lenient().when(appProperties.getMinio()).thenReturn(new AppProperties.Minio());

        reportService.submitReport(auditId, "new-key", "new.pdf");
        assertEquals(ReportStatus.PENDING_REVIEW, existing.getStatus());
        assertEquals("new-key", existing.getDocumentFileKey());
        assertNull(existing.getReviewComment());
    }

    // --- reviewReport ---

    @Test
    @DisplayName("reviewReport APPROVE sets APPROVED and COMPLETED")
    void reviewReport_Approve() {
        mockSecurityContext(managerUser);
        Report report = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.PENDING_REVIEW).build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(managerUser.getId())).thenReturn(Optional.of(managerUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenReturn(report);
        lenient().when(appProperties.getMinio()).thenReturn(new AppProperties.Minio());

        ReportResponse r = reportService.reviewReport(auditId, "APPROVE", "Looks good");
        assertEquals(ReportStatus.APPROVED, report.getStatus());
        assertEquals(AuditStatus.COMPLETED, audit.getStatus());
        verify(auditRepository).save(audit);
        verify(notificationService, times(2)).push(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reviewReport REJECT sets REJECTED status")
    void reviewReport_Reject() {
        mockSecurityContext(managerUser);
        Report report = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.PENDING_REVIEW).build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(managerUser.getId())).thenReturn(Optional.of(managerUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenReturn(report);
        lenient().when(appProperties.getMinio()).thenReturn(new AppProperties.Minio());

        reportService.reviewReport(auditId, "REJECT", "Needs work");
        assertEquals(ReportStatus.REJECTED, report.getStatus());
        assertEquals("Needs work", report.getReviewComment());
    }

    @Test
    @DisplayName("reviewReport REVISION sets REVISION_REQUESTED")
    void reviewReport_Revision() {
        mockSecurityContext(managerUser);
        Report report = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.PENDING_REVIEW).build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(managerUser.getId())).thenReturn(Optional.of(managerUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenReturn(report);
        lenient().when(appProperties.getMinio()).thenReturn(new AppProperties.Minio());

        reportService.reviewReport(auditId, "REVISION", "Fix page 3");
        assertEquals(ReportStatus.REVISION_REQUESTED, report.getStatus());
    }

    @Test
    @DisplayName("reviewReport with invalid decision throws")
    void reviewReport_InvalidDecision() {
        mockSecurityContext(managerUser);
        Report report = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser).build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(managerUser.getId())).thenReturn(Optional.of(managerUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.of(report));
        assertThrows(ApiException.class, () -> reportService.reviewReport(auditId, "INVALID", null));
    }

    @Test
    @DisplayName("reviewReport throws when report not found")
    void reviewReport_NoReport() {
        mockSecurityContext(managerUser);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(managerUser.getId())).thenReturn(Optional.of(managerUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> reportService.reviewReport(auditId, "APPROVE", null));
    }

    // --- getByAuditId ---

    @Test
    @DisplayName("getByAuditId returns report response")
    void getByAuditId_Success() {
        Report report = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.READY).fileName("report.pdf").fileKey("fk").build();
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.of(report));
        when(appProperties.getMinio()).thenReturn(new AppProperties.Minio());
        when(storageService.presignedGetUrl(any(), eq("fk"))).thenReturn("http://url");

        ReportResponse r = reportService.getByAuditId(auditId);
        assertNotNull(r);
        assertEquals(ReportStatus.READY, r.getStatus());
    }

    @Test
    @DisplayName("getByAuditId throws when not found")
    void getByAuditId_NotFound() {
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> reportService.getByAuditId(auditId));
    }

    // --- generateReport ---

    @Test
    @DisplayName("generateReport creates PDF and marks audit COMPLETED")
    void generateReport_Success() {
        mockSecurityContext(auditorUser);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(auditorUser.getId())).thenReturn(Optional.of(auditorUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.empty());

        AppProperties.Minio minio = new AppProperties.Minio();
        when(appProperties.getMinio()).thenReturn(minio);
        when(storageService.uploadDocumentBytes(any(), any(), any(), any(), any())).thenReturn("pdf-key");
        when(storageService.presignedGetUrl(any(), eq("pdf-key"))).thenReturn("http://pdf-url");

        Report saved = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.READY).fileKey("pdf-key").fileName("Audit_Report_" + auditId + ".pdf").build();
        when(reportRepository.save(any())).thenReturn(saved);

        ReportResponse r = reportService.generateReport(auditId);
        assertNotNull(r);
        assertEquals(AuditStatus.COMPLETED, audit.getStatus());
        verify(notificationService).push(eq(clientUser), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("generateReport handles PDF generation failure")
    void generateReport_PdfFailure() {
        mockSecurityContext(auditorUser);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(auditorUser.getId())).thenReturn(Optional.of(auditorUser));
        when(reportRepository.findByAuditId(auditId)).thenReturn(Optional.empty());

        AppProperties.Minio minio = new AppProperties.Minio();
        when(appProperties.getMinio()).thenReturn(minio);
        when(storageService.uploadDocumentBytes(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("Storage down"));

        Report saved = Report.builder().id(UUID.randomUUID()).audit(audit).generatedBy(auditorUser)
                .status(ReportStatus.GENERATING).build();
        when(reportRepository.save(any())).thenReturn(saved);

        assertThrows(ApiException.class, () -> reportService.generateReport(auditId));
    }
}
