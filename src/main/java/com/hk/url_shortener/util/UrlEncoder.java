package com.hk.url_shortener.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

public class UrlEncoder {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = ALPHABET.length();

    // Method 1: Base62 encoding of incremental counter
    public static String encode(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    // Method 2: Generate short code from URL using hash
    public static String generateShortUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            // Take first 8 bytes and encode with Base64
            byte[] shorterHash = new byte[8];
            System.arraycopy(hash, 0, shorterHash, 0, 8);
            String base64 = Base64.getUrlEncoder().encodeToString(shorterHash);
            // Remove padding '=' characters and limit length
            return base64.replace("=", "").substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to random UUID
            return generateRandomShortUrl();
        }
    }

    // Method 3: Random short URL generation
    public static String generateRandomShortUrl() {
        StringBuilder sb = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // Take first 6 characters and map to our alphabet
        for (int i = 0; i < 6; i++) {
            int index = Integer.parseInt(uuid.substring(i, i + 1), 16) % BASE;
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}
