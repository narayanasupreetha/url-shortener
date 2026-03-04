package com.urlshortener.controller;

import com.urlshortener.config.SecurityConfig;
import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.exception.LinkGoneException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.service.CustomUserDetailsService;
import com.urlshortener.service.RedirectService;
import com.urlshortener.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LWFsZ29yaXRobQ==",
        "jwt.expirationMs=3600000"
})
class RedirectControllerTest {

    private static final String BASE_URL = "/api/v1/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedirectService redirectService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;


    @Test
    @DisplayName("GET /{shortCode} → 302 Found with Location header pointing to original URL")
    void redirect_validShortCode_returns302WithLocation() throws Exception {
        String shortCode = "abc123";
        String originalUrl = "https://www.example.com/some/long/path";

        when(redirectService.resolveAndTrack(eq(shortCode), any()))
                .thenReturn(originalUrl);

        mockMvc.perform(get(BASE_URL + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));

        verify(redirectService, times(1)).resolveAndTrack(eq(shortCode), any());
    }


    @Test
    @DisplayName("GET /{shortCode} → 404 Not Found when short code does not exist")
    void redirect_unknownShortCode_returns404() throws Exception {
        String shortCode = "unknown";

        when(redirectService.resolveAndTrack(eq(shortCode), any()))
                .thenThrow(new ResourceNotFoundException("Short URL not found"));

        mockMvc.perform(get(BASE_URL + shortCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT FOUND"));

        verify(redirectService, times(1)).resolveAndTrack(eq(shortCode), any());
    }


    @Test
    @DisplayName("GET /{shortCode} → 410 Gone when the short URL has been deactivated")
    void redirect_inactiveShortUrl_returns410() throws Exception {
        String shortCode = "old123";

        when(redirectService.resolveAndTrack(eq(shortCode), any()))
                .thenThrow(new LinkGoneException("Short URL is inactive"));

        mockMvc.perform(get(BASE_URL + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GONE"))
                .andExpect(jsonPath("$.message").value("The short link is no longer available"));

        verify(redirectService, times(1)).resolveAndTrack(eq(shortCode), any());
    }

    @Test
    @DisplayName("GET /{shortCode} → 410 Gone when the short URL has expired")
    void redirect_expiredShortUrl_returns410() throws Exception {
        String shortCode = "exp123";

        when(redirectService.resolveAndTrack(eq(shortCode), any()))
                .thenThrow(new LinkGoneException("Short URL has expired"));

        mockMvc.perform(get(BASE_URL + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("GONE"));

        verify(redirectService, times(1)).resolveAndTrack(eq(shortCode), any());
    }
}

