package com.audit.platform.config;

import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.domain.UserStatus;
import com.audit.platform.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername returns SecurityUserDetails for existing user")
    void loadUserByUsername_Found() {
        User user = User.builder().id(UUID.randomUUID()).email("test@e.com").passwordHash("hash")
                .role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmailIgnoreCase("test@e.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@e.com");
        assertNotNull(details);
        assertEquals("test@e.com", details.getUsername());
        assertTrue(details instanceof SecurityUserDetails);
        assertTrue(details.isEnabled());
    }

    @Test
    @DisplayName("loadUserByUsername throws for non-existent user")
    void loadUserByUsername_NotFound() {
        when(userRepository.findByEmailIgnoreCase("nope@e.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nope@e.com"));
    }
}
