package com.hk.url_shortener.service;

import com.hk.url_shortener.model.Url;
import com.hk.url_shortener.repository.UrlRepository;
import com.hk.url_shortener.util.UrlEncoder;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UrlService {

    @Autowired
    private UrlRepository urlRepository;

    @Value("${app.base.url}")
    private String baseUrl;

    /**
     * Creates a shortened URL
     */
    @Transactional
    public String shortenUrl(String originalUrl, Integer expirationDays) {
        // Basic URL validation
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        // Generate short URL code
        String shortUrl = UrlEncoder.generateShortUrl(originalUrl);

        // Check for collisions and regenerate if needed
        int attempts = 0;
        while (urlRepository.existsByShortUrl(shortUrl) && attempts < 5) {
            shortUrl = UrlEncoder.generateRandomShortUrl();
            attempts++;
        }

        // If still colliding after attempts, use counter-based approach
        if (urlRepository.existsByShortUrl(shortUrl)) {
            long count = urlRepository.count() + 1;
            shortUrl = UrlEncoder.encode(count);
        }

        // Set expiration if provided
        LocalDateTime expiresAt = null;
        if (expirationDays != null && expirationDays > 0) {
            expiresAt = LocalDateTime.now().plusDays(expirationDays);
        }

        // Save the URL mapping
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortUrl(shortUrl);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(expiresAt);
        urlRepository.save(url);

        return baseUrl + shortUrl;
    }

    /**
     * Retrieves the original URL from a shortened URL
     */
    @Transactional
    public Optional<String> getOriginalUrl(String shortUrl) {
        Optional<Url> urlOpt = urlRepository.findByShortUrl(shortUrl);

        if (urlOpt.isPresent()) {
            Url url = urlOpt.get();

            // Check if URL has expired
            if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
                return Optional.empty();
            }

            // Increment click count
            url.setClickCount(url.getClickCount() + 1);
            urlRepository.save(url);

            return Optional.of(url.getOriginalUrl());
        }

        return Optional.empty();
    }

    /**
     * Basic URL validation
     */
    private boolean isValidUrl(String url) {
        return url != null &&
                (url.startsWith("http://") || url.startsWith("https://")) &&
                url.length() >= 10;
    }
}