package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures a Caffeine in-memory cache.
 * The "shortUrls" cache is used by RedirectService to avoid
 * hitting the database on every redirect. Frequently used
 * short codes are served from memory until they expire
 * or are removed.
 */
@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${app.cache.short-urls.max-size:10000}")
    private long shortUrlsMaxSize;

    @Value("${app.cache.short-urls.expire-after-write-minutes:5}")
    private long shortUrlsExpireAfterWriteMinutes;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("shortUrls");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(shortUrlsMaxSize)
                .expireAfterWrite(shortUrlsExpireAfterWriteMinutes, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}

