package com.audit.platform.modules.form.controller;

import com.audit.platform.modules.form.dto.FormResponse;
import com.audit.platform.modules.form.dto.SubmitFormRequest;
import com.audit.platform.modules.form.service.FormService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormController Unit Tests")
class FormControllerTest {

    @Mock private FormService formService;
    @InjectMocks private FormController controller;

    @Test
    @DisplayName("submit returns form response")
    void submit() {
        SubmitFormRequest req = new SubmitFormRequest();
        FormResponse resp = FormResponse.builder().id(UUID.randomUUID()).build();
        when(formService.submit(req)).thenReturn(resp);

        ResponseEntity<FormResponse> r = controller.submit(req);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(resp.getId(), r.getBody().getId());
    }

    @Test
    @DisplayName("get returns form by audit ID")
    void get() {
        UUID id = UUID.randomUUID();
        FormResponse resp = FormResponse.builder().id(UUID.randomUUID()).build();
        when(formService.getByAuditId(id)).thenReturn(resp);

        ResponseEntity<FormResponse> r = controller.get(id);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(resp.getId(), r.getBody().getId());
    }
}
