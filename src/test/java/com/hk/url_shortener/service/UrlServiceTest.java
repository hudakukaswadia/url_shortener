package com.hk.url_shortener.service;

import com.hk.url_shortener.model.Url;
import com.hk.url_shortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080/");
    }

    @Test
    void shortenUrl_ValidUrl_ReturnsShortenedUrl() {
        // Arrange
        String originalUrl = "https://example.com/very/long/url";
        when(urlRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        String shortenedUrl = urlService.shortenUrl(originalUrl, null);

        // Assert
        assertNotNull(shortenedUrl);
        assertTrue(shortenedUrl.startsWith("http://localhost:8080/"));
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void shortenUrl_InvalidUrl_ThrowsException() {
        // Arrange
        String invalidUrl = "not-a-url";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.shortenUrl(invalidUrl, null);
        });

        assertEquals("Invalid URL format", exception.getMessage());
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void getOriginalUrl_ValidShortUrl_ReturnsOriginalUrl() {
        // Arrange
        String shortUrl = "abc123";
        String originalUrl = "https://example.com/long/url";

        Url url = new Url();
        url.setShortUrl(shortUrl);
        url.setOriginalUrl(originalUrl);
        url.setCreatedAt(LocalDateTime.now());

        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(url);

        // Act
        Optional<String> result = urlService.getOriginalUrl(shortUrl);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(originalUrl, result.get());
        verify(urlRepository).save(any(Url.class)); // Verify click count was updated
    }

    @Test
    void getOriginalUrl_ExpiredUrl_ReturnsEmpty() {
        // Arrange
        String shortUrl = "abc123";

        Url url = new Url();
        url.setShortUrl(shortUrl);
        url.setOriginalUrl("https://example.com");
        url.setCreatedAt(LocalDateTime.now().minusDays(2));
        url.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired

        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.of(url));

        // Act
        Optional<String> result = urlService.getOriginalUrl(shortUrl);

        // Assert
        assertTrue(result.isEmpty());
        verify(urlRepository, never()).save(any(Url.class)); // No update since it's expired
    }

    @Test
    void getOriginalUrl_NonExistentShortUrl_ReturnsEmpty() {
        // Arrange
        String shortUrl = "nonexistent";
        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.empty());

        // Act
        Optional<String> result = urlService.getOriginalUrl(shortUrl);

        // Assert
        assertTrue(result.isEmpty());
    }
}