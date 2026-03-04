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
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private UrlService urlService;


    @Test
    @DisplayName("createShortUrl → returns ShortUrlResponse with shortCode when request is valid")
    void createShortUrl_validRequest_returnsUrlResponse() throws Exception {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://short.ly");

        User user = User.builder().id(1L).email("user@example.com").build();
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com/long/path", null);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(shortCodeGenerator.generate()).thenReturn("abc123");
        when(urlRepository.existsByShortCode("abc123")).thenReturn(false);
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortUrlResponse response = urlService.createShortUrl(request, "user@example.com");

        assertThat(response.shortCode()).isEqualTo("abc123");
        assertThat(response.shortUrl()).isEqualTo("http://short.ly/abc123");
        assertThat(response.originalUrl()).isEqualTo("https://www.example.com/long/path");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("createShortUrl → saves ShortUrl with correct fields")
    void createShortUrl_validRequest_savesShortUrlWithCorrectFields() throws Exception {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://short.ly");

        User user = User.builder().id(1L).email("user@example.com").build();
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com", expiry);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(shortCodeGenerator.generate()).thenReturn("xyz999");
        when(urlRepository.existsByShortCode("xyz999")).thenReturn(false);
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        urlService.createShortUrl(request, "user@example.com");

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(urlRepository).save(captor.capture());

        ShortUrl saved = captor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("xyz999");
        assertThat(saved.getOriginalUrl()).isEqualTo("https://www.example.com");
        assertThat(saved.getExpiresAt()).isEqualTo(expiry);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("createShortUrl → retries code generation until a unique code is found")
    void createShortUrl_codeCollision_retriesUntilUniqueCode() throws Exception {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://short.ly");

        User user = User.builder().id(1L).email("user@example.com").build();
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com", null);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        // First two codes collide, third is unique
        when(shortCodeGenerator.generate()).thenReturn("dup001", "dup002", "uniq03");
        when(urlRepository.existsByShortCode("dup001")).thenReturn(true);
        when(urlRepository.existsByShortCode("dup002")).thenReturn(true);
        when(urlRepository.existsByShortCode("uniq03")).thenReturn(false);
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortUrlResponse response = urlService.createShortUrl(request, "user@example.com");

        assertThat(response.shortCode()).isEqualTo("uniq03");
        verify(shortCodeGenerator, times(3)).generate();
    }

    @Test
    @DisplayName("createShortUrl → throws BadRequestException when the URL format is invalid")
    void createShortUrl_invalidUrl_throwsBadRequestException() {
        CreateUrlRequest request = new CreateUrlRequest("not-a-valid-url", null);

        assertThatThrownBy(() -> urlService.createShortUrl(request, "user@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid URL format");

        verifyNoInteractions(urlRepository, userRepository, shortCodeGenerator);
    }

    @Test
    @DisplayName("createShortUrl → throws ResourceNotFoundException when user email does not exist")
    void createShortUrl_userNotFound_throwsResourceNotFoundException() {
        CreateUrlRequest request = new CreateUrlRequest("https://www.example.com", null);

        // validateUrl passes, then findByEmail is called before generateUniqueCode,
        // so no code-generation stubs are needed here.
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.createShortUrl(request, "ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(urlRepository, never()).save(any());
        verifyNoInteractions(shortCodeGenerator);
    }

    @Test
    @DisplayName("getUserUrls → returns PagedResponse with correct metadata and content")
    void getUserUrls_validUser_returnsPagedResponse() {
        User user = User.builder().id(1L).email("user@example.com").build();
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L).shortCode("abc123").originalUrl("https://www.example.com")
                .isActive(true).createdAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(urlRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shortUrl)));

        PagedResponse<ShortUrlResponse> response = urlService.getUserUrls("user@example.com", 0, 10);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).shortCode()).isEqualTo("abc123");
        assertThat(response.pageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("getUserUrls → clamps negative page to 0")
    void getUserUrls_negativePage_clampsToZero() {
        User user = User.builder().id(1L).email("user@example.com").build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(urlRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<ShortUrlResponse> response = urlService.getUserUrls("user@example.com", -5, 10);

        assertThat(response.pageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("getUserUrls → clamps size larger than 100 to 100")
    void getUserUrls_oversizedPage_clampsTo100() {
        User user = User.builder().id(1L).email("user@example.com").build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(urlRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<ShortUrlResponse> response = urlService.getUserUrls("user@example.com", 0, 200);

        assertThat(response.pageSize()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("getUserUrls → throws RuntimeException when user does not exist")
    void getUserUrls_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getUserUrls("ghost@example.com", 0, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("deactivateUrl → sets isActive to false for the given URL")
    void deactivateUrl_validOwner_setsActiveToFalse() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L).shortCode("abc123").isActive(true).build();

        when(urlRepository.findByIdAndUserEmail(1L, "user@example.com"))
                .thenReturn(Optional.of(shortUrl));
        when(cacheManager.getCache("shortUrls")).thenReturn(cache);

        urlService.deactivateUrl(1L, "user@example.com");

        assertThat(shortUrl.isActive()).isFalse();
        verify(cache).evict("abc123");
    }

    @Test
    @DisplayName("deactivateUrl → throws RuntimeException when URL does not belong to the user")
    void deactivateUrl_urlNotOwnedByUser_throwsRuntimeException() {
        when(urlRepository.findByIdAndUserEmail(99L, "user@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.deactivateUrl(99L, "user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URL not found");
    }
}

