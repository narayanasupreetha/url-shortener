package com.urlshortener.service;

import com.urlshortener.entity.ShortUrl;
import com.urlshortener.exception.LinkGoneException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ClickEventService clickEventService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private RedirectService redirectService;

    @Test
    @DisplayName("resolveAndTrack → returns original URL and fires async click tracking")
    void resolveAndTrack_validCode_returnsOriginalUrlAndTracksClick() {
        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode("abc123")
                .originalUrl("https://www.example.com/some/long/path")
                .isActive(true)
                .expiresAt(null)
                .build();

        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));
        when(applicationContext.getBean(RedirectService.class)).thenReturn(redirectService);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getHeader("Referer")).thenReturn(null);

        String result = redirectService.resolveAndTrack("abc123", httpServletRequest);

        assertThat(result).isEqualTo("https://www.example.com/some/long/path");
        verify(clickEventService, times(1))
                .recordClickAsync(shortUrl, "192.168.1.1", "Mozilla/5.0", null);
    }

    @Test
    @DisplayName("resolveAndTrack → tracks click even when URL has a future expiry date")
    void resolveAndTrack_futureExpiry_returnsOriginalUrl() {
        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode("fut123")
                .originalUrl("https://www.example.com")
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(urlRepository.findByShortCode("fut123")).thenReturn(Optional.of(shortUrl));
        when(applicationContext.getBean(RedirectService.class)).thenReturn(redirectService);
        when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("TestAgent");
        when(httpServletRequest.getHeader("Referer")).thenReturn("https://referrer.com");

        String result = redirectService.resolveAndTrack("fut123", httpServletRequest);

        assertThat(result).isEqualTo("https://www.example.com");
        verify(clickEventService)
                .recordClickAsync(shortUrl, "10.0.0.1", "TestAgent", "https://referrer.com");
    }


    @Test
    @DisplayName("resolveAndTrack → throws ResourceNotFoundException when short code does not exist")
    void resolveAndTrack_unknownCode_throwsResourceNotFoundException() {
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());
        when(applicationContext.getBean(RedirectService.class)).thenReturn(redirectService);

        assertThatThrownBy(() -> redirectService.resolveAndTrack("unknown", httpServletRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Short URL not found");

        verifyNoInteractions(clickEventService);
    }

    @Test
    @DisplayName("resolveAndTrack → throws LinkGoneException when short URL is inactive")
    void resolveAndTrack_inactiveUrl_throwsLinkGoneException() {
        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode("old123")
                .originalUrl("https://www.example.com")
                .isActive(false)
                .build();

        when(urlRepository.findByShortCode("old123")).thenReturn(Optional.of(shortUrl));
        when(applicationContext.getBean(RedirectService.class)).thenReturn(redirectService);

        assertThatThrownBy(() -> redirectService.resolveAndTrack("old123", httpServletRequest))
                .isInstanceOf(LinkGoneException.class)
                .hasMessageContaining("inactive");

        verifyNoInteractions(clickEventService);
    }

    @Test
    @DisplayName("resolveAndTrack → throws LinkGoneException when short URL has expired")
    void resolveAndTrack_expiredUrl_throwsLinkGoneException() {
        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode("exp123")
                .originalUrl("https://www.example.com")
                .isActive(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(urlRepository.findByShortCode("exp123")).thenReturn(Optional.of(shortUrl));
        when(applicationContext.getBean(RedirectService.class)).thenReturn(redirectService);

        assertThatThrownBy(() -> redirectService.resolveAndTrack("exp123", httpServletRequest))
                .isInstanceOf(LinkGoneException.class)
                .hasMessageContaining("expired");

        verifyNoInteractions(clickEventService);
    }
}
