package com.audit.platform.modules.form.service;

import com.audit.platform.config.SecurityUserDetails;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.repository.AuditRepository;
import com.audit.platform.modules.form.domain.AuditForm;
import com.audit.platform.modules.form.dto.FormResponse;
import com.audit.platform.modules.form.dto.SubmitFormRequest;
import com.audit.platform.modules.form.repository.AuditFormRepository;
import com.audit.platform.modules.notification.service.NotificationService;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.repository.UserRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormService Unit Tests")
class FormServiceTest {

    @Mock private AuditFormRepository formRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private FormService formService;

    private User clientUser;
    private Audit audit;
    private UUID auditId;

    @BeforeEach
    void setUp() {
        clientUser = User.builder().id(UUID.randomUUID()).email("c@t.com").fullName("Client").role(UserRole.CLIENT).build();
        auditId = UUID.randomUUID();
        audit = Audit.builder().id(auditId).title("Test Audit").client(clientUser).status(AuditStatus.DRAFT).build();
        mockSecurityContext(clientUser);
    }

    private void mockSecurityContext(User user) {
        SecurityUserDetails details = new SecurityUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Submit form creates new form and moves audit to PENDING")
    void submit_NewForm_DraftToPending() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(userRepository.findByRole(UserRole.MANAGER)).thenReturn(List.of());

        AuditForm saved = AuditForm.builder().id(UUID.randomUUID()).audit(audit)
                .companyName("Test Co").legalForm("SARL").revenue(BigDecimal.valueOf(1000000))
                .submittedAt(Instant.now()).build();
        when(formRepository.save(any())).thenReturn(saved);

        SubmitFormRequest req = new SubmitFormRequest();
        req.setAuditId(auditId);
        req.setCompanyName("Test Co");
        req.setLegalForm("SARL");
        req.setRevenue(1000000.0);

        FormResponse r = formService.submit(req);

        assertNotNull(r);
        assertEquals("Test Co", r.getCompanyName());
        assertEquals(AuditStatus.PENDING, audit.getStatus());
        verify(auditRepository).save(audit);
    }

    @Test
    @DisplayName("Submit form updates existing form")
    void submit_UpdatesExisting() {
        audit.setStatus(AuditStatus.IN_PROGRESS); // not DRAFT, so no status change
        AuditForm existing = AuditForm.builder().id(UUID.randomUUID()).audit(audit).companyName("Old").build();
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.of(existing));
        when(formRepository.save(any())).thenReturn(existing);
        when(userRepository.findByRole(UserRole.MANAGER)).thenReturn(List.of());

        SubmitFormRequest req = new SubmitFormRequest();
        req.setAuditId(auditId);
        req.setCompanyName("Updated Co");

        formService.submit(req);

        assertEquals("Updated Co", existing.getCompanyName());
    }

    @Test
    @DisplayName("Submit form with financial data serializes to JSON")
    void submit_WithFinancialData() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        when(userRepository.findByRole(UserRole.MANAGER)).thenReturn(List.of());

        AuditForm saved = AuditForm.builder().id(UUID.randomUUID()).audit(audit)
                .financialData("{\"revenue\":1000}").submittedAt(Instant.now()).build();
        when(formRepository.save(any())).thenReturn(saved);

        SubmitFormRequest req = new SubmitFormRequest();
        req.setAuditId(auditId);
        req.setFinancialData(Map.of("revenue", 1000));

        FormResponse r = formService.submit(req);
        assertNotNull(r);
    }

    @Test
    @DisplayName("Submit form throws when audit not found")
    void submit_AuditNotFound() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.empty());
        SubmitFormRequest req = new SubmitFormRequest();
        req.setAuditId(auditId);
        assertThrows(ApiException.class, () -> formService.submit(req));
    }

    @Test
    @DisplayName("getByAuditId returns form response")
    void getByAuditId_Success() {
        AuditForm form = AuditForm.builder().id(UUID.randomUUID()).audit(audit)
                .companyName("Test Co").revenue(BigDecimal.valueOf(500000))
                .submittedAt(Instant.now()).build();
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.of(form));
        FormResponse r = formService.getByAuditId(auditId);
        assertNotNull(r);
        assertEquals("Test Co", r.getCompanyName());
        assertEquals(500000.0, r.getRevenue());
    }

    @Test
    @DisplayName("getByAuditId throws when form not found")
    void getByAuditId_NotFound() {
        when(formRepository.findByAuditId(auditId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> formService.getByAuditId(auditId));
    }
}
