package com.hk.url_shortener.controller;

import com.hk.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.util.Optional;

@Controller
public class UrlController {

    @Autowired
    private UrlService urlService;

    // Home page with form
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Redirect to original URL
    @GetMapping("/{shortUrl}")
    public RedirectView redirectToOriginalUrl(@PathVariable String shortUrl) {
        Optional<String> originalUrl = urlService.getOriginalUrl(shortUrl);

        if (originalUrl.isPresent()) {
            return new RedirectView(originalUrl.get());
        } else {
            // Redirect to error page or home page
            return new RedirectView("/?error=url_not_found");
        }
    }


    // REST API endpoint to create short URL
    @PostMapping("/api/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody @Valid UrlRequest request) {
        try {
            String shortUrl = urlService.shortenUrl(request.getUrl(), request.getExpirationDays());
            return ResponseEntity.ok(new UrlResponse(shortUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Data transfer objects
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UrlRequest {
        @NotBlank(message = "URL cannot be empty")
        private String url;
        private Integer expirationDays;
    }

    @Data
    @AllArgsConstructor
    public static class UrlResponse {
        private String shortUrl;
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }
}