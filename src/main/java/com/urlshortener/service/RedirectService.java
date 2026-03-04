package com.urlshortener.service;

import com.urlshortener.entity.ShortUrl;
import com.urlshortener.exception.LinkGoneException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


/**
 * Service responsible for handling URL redirection and click tracking.
 * It:
 * - Resolves short codes to original URLs
 * - Validates active/expiry status
 * - Records click events asynchronously
 */
@Service
@RequiredArgsConstructor
public class RedirectService {

    private final UrlRepository urlRepository;
    private final ClickEventService clickEventService;
    private final ApplicationContext applicationContext;

    /**
     * Resolves a short URL and records the click event.
     * - Fetches active short URL
     * - Validates expiry and status
     * - Records click details asynchronously
     * - Returns the original URL for redirection
     *
     * @param shortCode unique short URL code
     * @param request   HTTP request containing client details
     * @return original URL
     */
    public String resolveAndTrack(String shortCode, HttpServletRequest request) {

        RedirectService self = applicationContext.getBean(RedirectService.class);
        ShortUrl shortUrl = self.findActiveShortUrl(shortCode);

        if (shortUrl.getExpiresAt() != null &&
                shortUrl.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LinkGoneException("Short URL has expired");
        }

        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        clickEventService.recordClickAsync(shortUrl, ipAddress, userAgent, referrer);

        return shortUrl.getOriginalUrl();
    }

    /**
     * Fetches an active short URL from cache or database.
     * Results are cached to improve redirect performance.
     *
     * @param shortCode short URL code
     * @return active ShortUrl entity
     * @throws ResourceNotFoundException if not found
     * @throws LinkGoneException         if inactive
     */
    @Cacheable(value = "shortUrls", key = "#shortCode")
    public ShortUrl findActiveShortUrl(String shortCode) {
        ShortUrl shortUrl = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));

        if (!shortUrl.isActive()) {
            throw new LinkGoneException("Short URL is inactive");
        }

        return shortUrl;
    }
}
