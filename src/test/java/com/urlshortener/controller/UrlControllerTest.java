package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.config.SecurityConfig;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.PagedResponse;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.service.CustomUserDetailsService;
import com.urlshortener.service.UrlService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LWFsZ29yaXRobQ==",
        "jwt.expirationMs=3600000"
})
class UrlControllerTest {

    private static final String BASE_URL = "/api/v1/urls";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;


    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("POST /urls → 201 Created when authenticated USER submits a valid URL")
    void createUrl_asUser_validRequest_returns201() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com/some/long/path", null);
        ShortUrlResponse response = new ShortUrlResponse(
                1L, "http://short.ly/abc123", "abc123",
                "https://www.example.com/some/long/path",
                LocalDateTime.now(), null, true
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class), eq("user@example.com")))
                .thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/some/long/path"))
                .andExpect(jsonPath("$.active").value(true));

        verify(urlService, times(1)).createShortUrl(any(CreateUrlRequest.class), eq("user@example.com"));
    }


    @Test
    @DisplayName("POST /urls → 403 Forbidden when request has no authentication")
    void createUrl_unauthenticated_returns403() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com", null);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(urlService);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("POST /urls → 201 Created when ADMIN user submits a valid URL (method security not enforced)")
    void createUrl_asAdmin_returns201() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com", null);
        ShortUrlResponse response = new ShortUrlResponse(
                1L, "http://short.ly/xyz999", "xyz999",
                "https://www.example.com",
                LocalDateTime.now(), null, true
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class), eq("admin@example.com")))
                .thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("xyz999"));

        verify(urlService, times(1)).createShortUrl(any(CreateUrlRequest.class), eq("admin@example.com"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("POST /urls → 400 Bad Request when originalUrl is blank")
    void createUrl_blankUrl_returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("", null);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(urlService);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("POST /urls → 400 Bad Request when expiryDate is in the past")
    void createUrl_pastExpiryDate_returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com",
                LocalDateTime.now().minusDays(1)
        );

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(urlService);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("GET /urls → 200 OK with paged list of short URLs for authenticated USER")
    void listUserUrls_asUser_returns200() throws Exception {
        ShortUrlResponse item = new ShortUrlResponse(
                1L, "http://short.ly/abc123", "abc123", "https://www.example.com",
                LocalDateTime.now(), null, true
        );
        PagedResponse<ShortUrlResponse> page = new PagedResponse<>(0, 10, 1, 1, List.of(item));

        when(urlService.getUserUrls(eq("user@example.com"), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].shortCode").value("abc123"));

        verify(urlService, times(1)).getUserUrls(eq("user@example.com"), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /urls → 403 Forbidden when request has no authentication")
    void listUserUrls_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());

        verifyNoInteractions(urlService);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("GET /urls → 400 Bad Request when size param is less than 1")
    void listUserUrls_invalidSize_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlService);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("PATCH /urls/{id}/deactivate → 204 No Content when USER deactivates their own URL")
    void deactivateUrl_asOwner_returns204() throws Exception {
        doNothing().when(urlService).deactivateUrl(eq(1L), eq("user@example.com"));

        mockMvc.perform(patch(BASE_URL + "/1/deactivate")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(urlService, times(1)).deactivateUrl(eq(1L), eq("user@example.com"));
    }


    @Test
    @DisplayName("PATCH /urls/{id}/deactivate → 403 Forbidden when request has no authentication")
    void deactivateUrl_unauthenticated_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/1/deactivate")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(urlService);
    }
}

