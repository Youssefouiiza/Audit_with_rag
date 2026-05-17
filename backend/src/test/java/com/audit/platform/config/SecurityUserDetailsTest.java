package com.audit.platform.config;

import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityUserDetails Unit Tests")
class SecurityUserDetailsTest {

    @Test
    @DisplayName("Constructor extracts all fields from User")
    void constructor_ExtractsFields() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("test@e.com").passwordHash("hash123")
                .role(UserRole.ADMIN).status(UserStatus.ACTIVE).firstLogin(true).build();

        SecurityUserDetails details = new SecurityUserDetails(user);

        assertEquals(userId, details.getId());
        assertEquals("test@e.com", details.getEmail());
        assertEquals("test@e.com", details.getUsername());
        assertEquals("hash123", details.getPassword());
        assertEquals("hash123", details.getPasswordHash());
        assertTrue(details.isFirstLogin());
        assertEquals(UserStatus.ACTIVE, details.getStatus());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("isEnabled returns true for ACTIVE status")
    void isEnabled_Active() {
        User user = User.builder().id(UUID.randomUUID()).email("e").passwordHash("h")
                .role(UserRole.CLIENT).status(UserStatus.ACTIVE).build();
        assertTrue(new SecurityUserDetails(user).isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false for INACTIVE status")
    void isEnabled_Inactive() {
        User user = User.builder().id(UUID.randomUUID()).email("e").passwordHash("h")
                .role(UserRole.CLIENT).status(UserStatus.INACTIVE).build();
        assertFalse(new SecurityUserDetails(user).isEnabled());
    }

    @Test
    @DisplayName("isAccountNonLocked returns false for SUSPENDED status")
    void isAccountNonLocked_Suspended() {
        User user = User.builder().id(UUID.randomUUID()).email("e").passwordHash("h")
                .role(UserRole.AUDITOR).status(UserStatus.SUSPENDED).build();
        assertFalse(new SecurityUserDetails(user).isAccountNonLocked());
    }

    @Test
    @DisplayName("isAccountNonLocked returns true for ACTIVE status")
    void isAccountNonLocked_Active() {
        User user = User.builder().id(UUID.randomUUID()).email("e").passwordHash("h")
                .role(UserRole.AUDITOR).status(UserStatus.ACTIVE).build();
        assertTrue(new SecurityUserDetails(user).isAccountNonLocked());
    }

    @Test
    @DisplayName("Authorities contain ROLE_ prefix with role name")
    void authorities_ContainRolePrefix() {
        User auditor = User.builder().id(UUID.randomUUID()).email("e").passwordHash("h")
                .role(UserRole.AUDITOR).status(UserStatus.ACTIVE).build();
        SecurityUserDetails details = new SecurityUserDetails(auditor);
        assertEquals(1, details.getAuthorities().size());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AUDITOR")));
    }

    @Test
    @DisplayName("Manager role authority is correct")
    void authorities_ManagerRole() {
        User mgr = User.builder().id(UUID.randomUUID()).email("e").passwordHash("h")
                .role(UserRole.MANAGER).status(UserStatus.ACTIVE).build();
        SecurityUserDetails details = new SecurityUserDetails(mgr);
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }
}
