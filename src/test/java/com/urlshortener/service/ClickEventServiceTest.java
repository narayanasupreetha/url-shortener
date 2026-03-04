package com.urlshortener.service;

import com.urlshortener.entity.ShortUrl;
import com.urlshortener.entity.UrlClickEvent;
import com.urlshortener.repository.UrlClickEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClickEventServiceTest {

    @Mock
    private UrlClickEventRepository repository;

    @InjectMocks
    private ClickEventService clickEventService;

    @Test
    @DisplayName("recordClickAsync → saves UrlClickEvent with IP address and User-Agent from request")
    void recordClickAsync_validRequest_savesClickEvent() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://www.example.com")
                .build();

        clickEventService.recordClickAsync(shortUrl, "192.168.1.1", "Mozilla/5.0", null);

        ArgumentCaptor<UrlClickEvent> captor = ArgumentCaptor.forClass(UrlClickEvent.class);
        verify(repository, times(1)).save(captor.capture());

        UrlClickEvent saved = captor.getValue();
        assertThat(saved.getShortUrl()).isEqualTo(shortUrl);
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getClickedAt()).isNotNull();
    }

    @Test
    @DisplayName("recordClickAsync → saves UrlClickEvent with null User-Agent when header is absent")
    void recordClickAsync_noUserAgent_savesClickEventWithNullUserAgent() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(2L)
                .shortCode("xyz999")
                .originalUrl("https://www.another.com")
                .build();

        clickEventService.recordClickAsync(shortUrl, "10.0.0.1", null, null);

        ArgumentCaptor<UrlClickEvent> captor = ArgumentCaptor.forClass(UrlClickEvent.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getUserAgent()).isNull();
        assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.1");
    }
}
