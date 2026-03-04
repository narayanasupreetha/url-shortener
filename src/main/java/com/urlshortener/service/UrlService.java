package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.PagedResponse;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.entity.User;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CacheManager cacheManager;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public ShortUrlResponse createShortUrl(CreateUrlRequest request, String userEmail) throws BadRequestException {

        validateUrl(request.originalUrl());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String shortCode = generateUniqueCode();

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(request.originalUrl())
                .shortCode(shortCode)
                .createdAt(LocalDateTime.now())
                .expiresAt(request.expiryDate())
                .isActive(true)
                .user(user)
                .build();

        urlRepository.save(shortUrl);

        return new ShortUrlResponse(
                shortUrl.getId(),
                baseUrl + "/" + shortCode,
                shortCode,
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.isActive()
        );
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = shortCodeGenerator.generate();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    private void validateUrl(String url) throws BadRequestException {
        try {
            new URL(url).toURI();
        } catch (Exception e) {
            throw new BadRequestException("Invalid URL format");
        }
    }

    public PagedResponse<ShortUrlResponse> getUserUrls(String username, int page, int size) {

        // Validate page
        if (page < 0) {
            page = 0;
        }

        // Validate size
        if (size < 1) {
            size = 10; // default fallback
        }

        // Optional: limit max size
        if (size > 100) {
            size = 100;
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ShortUrl> urlPage = urlRepository.findByUser(user, pageable);

        List<ShortUrlResponse> content = urlPage
                .map(url -> ShortUrlResponse.fromEntity(url, baseUrl))
                .getContent();

        return new PagedResponse<>(
                urlPage.getNumber(),
                urlPage.getSize(),
                urlPage.getTotalElements(),
                urlPage.getTotalPages(),
                content
        );
    }

    @Transactional
    public void deactivateUrl(Long id, String email) {

        ShortUrl shortUrl = urlRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        shortUrl.setActive(false);

        Cache cache = cacheManager.getCache("shortUrls");
        if (cache != null) {
            cache.evict(shortUrl.getShortCode());
        }
    }
}
