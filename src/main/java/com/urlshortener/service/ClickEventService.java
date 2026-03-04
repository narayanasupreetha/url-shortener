package com.urlshortener.service;

import com.urlshortener.entity.ShortUrl;
import com.urlshortener.entity.UrlClickEvent;
import com.urlshortener.repository.UrlClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for recording click events for shortened URLs.
 * Click events are stored asynchronously to avoid blocking the main request.
 */
@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final UrlClickEventRepository repository;

    /**
     * Records a click event asynchronously.
     * <p>
     * Creates a click event with request details like IP address,
     * user agent, and referrer, then saves it to the database.
     *
     * @param shortUrl  the shortened URL that was accessed
     * @param ipAddress client's IP address
     * @param userAgent browser/device information
     * @param referrer  source from where the request came
     */
    @Async
    public void recordClickAsync(ShortUrl shortUrl,
                                 String ipAddress,
                                 String userAgent,
                                 String referrer) {

        UrlClickEvent event = UrlClickEvent.builder()
                .shortUrl(shortUrl)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referrer(referrer)
                .clickedAt(LocalDateTime.now())
                .build();

        repository.save(event);
    }
}
