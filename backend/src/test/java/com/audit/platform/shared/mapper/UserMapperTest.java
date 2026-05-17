package com.audit.platform.shared.mapper;

import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserMapper Unit Tests")
class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    @DisplayName("toResponse maps fields correctly")
    void toResponse() {
        UUID userId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        User creator = User.builder().id(creatorId).build();
        User user = User.builder()
                .id(userId)
                .email("test@e.com")
                .role(UserRole.AUDITOR)
                .createdBy(creator)
                .build();

        UserResponse resp = mapper.toResponse(user);

        assertNotNull(resp);
        assertEquals(userId, resp.getId());
        assertEquals("test@e.com", resp.getEmail());
        assertEquals(UserRole.AUDITOR, resp.getRole());
        assertEquals(creatorId, resp.getCreatedById());
    }

    @Test
    @DisplayName("toResponse handles null user")
    void toResponse_Null() {
        assertNull(mapper.toResponse(null));
    }
}
