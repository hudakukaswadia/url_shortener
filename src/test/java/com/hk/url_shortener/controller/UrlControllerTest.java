package com.hk.url_shortener.controller;

import com.hk.url_shortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UrlControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlService urlService;

    @InjectMocks
    private UrlController urlController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(urlController).build();
    }

    @Test
    void home_ReturnsIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void redirectToOriginalUrl_ExistingUrl_RedirectsToOriginalUrl() throws Exception {
        // Arrange
        String shortUrl = "abc123";
        String originalUrl = "https://example.com";
        when(urlService.getOriginalUrl(shortUrl)).thenReturn(Optional.of(originalUrl));

        // Act & Assert
        mockMvc.perform(get("/{shortUrl}", shortUrl))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(originalUrl));
    }

    @Test
    void redirectToOriginalUrl_NonExistentUrl_RedirectsToErrorPage() throws Exception {
        // Arrange
        String shortUrl = "nonexistent";
        when(urlService.getOriginalUrl(shortUrl)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/{shortUrl}", shortUrl))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?error=url_not_found"));
    }

    @Test
    void shortenUrl_ValidUrl_ReturnsShortUrl() throws Exception {
        // Arrange
        String originalUrl = "https://example.com";
        String shortUrl = "http://localhost:8080/abc123";
        when(urlService.shortenUrl(anyString(), anyInt())).thenReturn(shortUrl);

        // Act & Assert
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + originalUrl + "\",\"expirationDays\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(shortUrl));
    }

    @Test
    void shortenUrl_InvalidUrl_ReturnsBadRequest() throws Exception {
        // Arrange
        String invalidUrl = "invalid-url";
        when(urlService.shortenUrl(anyString(), any())).thenThrow(new IllegalArgumentException("Invalid URL format"));

        // Act & Assert
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + invalidUrl + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid URL format"));
    }
}