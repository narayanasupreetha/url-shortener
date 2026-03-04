package com.urlshortener.service;

import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername → returns UserDetails with email and ROLE_USER authority")
    void loadUserByUsername_existingUser_returnsUserDetailsWithCorrectAuthority() {
        User user = User.builder()
                .id(1L)
                .email("bob@example.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("bob@example.com");

        assertThat(details.getUsername()).isEqualTo("bob@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("USER");
    }

    @Test
    @DisplayName("loadUserByUsername → returns UserDetails with ADMIN authority for an admin user")
    void loadUserByUsername_adminUser_returnsUserDetailsWithAdminAuthority() {
        User user = User.builder()
                .id(2L)
                .email("alice@example.com")
                .passwordHash("hashed-password")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("alice@example.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ADMIN");
    }

    // ─── User not found ───────────────────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername → throws UsernameNotFoundException when email does not exist")
    void loadUserByUsername_unknownEmail_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findByEmail("unknown@example.com");
    }
}

