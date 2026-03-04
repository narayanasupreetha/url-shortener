package com.urlshortener.service;

import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.LoginResponse;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.dto.RegisterResponse;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.exception.UserAlreadyExistsException;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register → returns RegisterResponse with email when request is valid")
    void register_validRequest_returnsRegisterResponse() {
        RegisterRequest request = new RegisterRequest("Bob", "bob@example.com", "Password1");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.email()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("register → saves user with USER role and encoded password")
    void register_validRequest_savesUserWithUserRoleAndEncodedPassword() {
        RegisterRequest request = new RegisterRequest("Bob", "bob@example.com", "Password1");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getEmail()).isEqualTo("bob@example.com");
    }


    @Test
    @DisplayName("register → throws UserAlreadyExistsException when email is already registered")
    void register_duplicateEmail_throwsUserAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("Bob", "bob@example.com", "Password1");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("bob@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login → returns LoginResponse with token, email and role on valid credentials")
    void login_validCredentials_returnsLoginResponse() {
        LoginRequest request = new LoginRequest("bob@example.com", "Password1");

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "bob@example.com",
                        "hashed-password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("bob@example.com")).thenReturn("jwt-token");

        // Reflect the @Value field (default 0 since @InjectMocks doesn't inject @Value)
        // Login response expiresIn = jwtExpirationMs / 1000 = 0 / 1000 = 0
        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("bob@example.com");
        assertThat(response.role()).isEqualTo("ROLE_USER");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("bob@example.com", "Password1")
        );
    }

    @Test
    @DisplayName("login → calls authenticationManager.authenticate with correct credentials")
    void login_callsAuthenticationManager() {
        LoginRequest request = new LoginRequest("bob@example.com", "Password1");

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "bob@example.com", "hashed-password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(anyString())).thenReturn("jwt-token");

        authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());

        assertThat(captor.getValue().getPrincipal()).isEqualTo("bob@example.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("Password1");
    }

    @Test
    @DisplayName("login → propagates BadCredentialsException when authenticationManager rejects credentials")
    void login_badCredentials_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("bob@example.com", "WrongPass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verify(jwtUtil, never()).generateToken(anyString());
    }
}

