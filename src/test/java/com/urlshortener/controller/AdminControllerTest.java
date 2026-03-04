package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.config.SecurityConfig;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.dto.RegisterResponse;
import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.exception.UserAlreadyExistsException;
import com.urlshortener.service.AdminService;
import com.urlshortener.service.CustomUserDetailsService;
import com.urlshortener.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LWFsZ29yaXRobQ==",
        "jwt.expirationMs=3600000"
})
class AdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/create";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    // Mocked so JwtFilter can be wired without a real UserRepository
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // Mocked so JwtFilter's token validation never requires a real secret at runtime
    @MockitoBean
    private JwtUtil jwtUtil;


    @Test
    @DisplayName("POST /create → 201 Created when admin user submits valid request")
    void createAdmin_asAdmin_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "Password1");
        RegisterResponse response = new RegisterResponse(1L, "alice@example.com");

        when(adminService.createAdmin(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(adminService, times(1)).createAdmin(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /create → 400 Bad Request when name is blank")
    void createAdmin_blankName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "alice@example.com", "Password1");

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /create → 400 Bad Request when email format is invalid")
    void createAdmin_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "not-an-email", "Password1");

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /create → 400 Bad Request when password is shorter than 8 characters")
    void createAdmin_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "Pass1");

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /create → 400 Bad Request when password contains only letters (no digit)")
    void createAdmin_passwordWithoutDigit_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "PasswordOnly");

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /create → 409 Conflict when email is already registered")
    void createAdmin_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "Password1");

        when(adminService.createAdmin(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("alice@example.com"));

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email already registered"));

        verify(adminService, times(1)).createAdmin(any(RegisterRequest.class));
    }
}
