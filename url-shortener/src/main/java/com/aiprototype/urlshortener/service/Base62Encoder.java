package com.aiprototype.urlshortener.service;

import org.springframework.stereotype.Component;

/**
 * Encodes a positive long to a Base62 string using digits + uppercase + lowercase.
 * Values in [62^5, 62^6) always produce exactly 6-character codes (56 billion combinations).
 */
@Component
public class Base62Encoder {

    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    // Inclusive bounds that guarantee a 6-character result
    static final long MIN_6_CHAR = 916_132_832L;   // 62^5
    static final long MAX_6_CHAR = 56_800_235_583L; // 62^6 - 1

    public String encode(long value) {
        if (value <= 0) throw new IllegalArgumentException("value must be positive");
        StringBuilder sb = new StringBuilder();
        long v = value;
        while (v > 0) {
            sb.append(ALPHABET.charAt((int) (v % BASE)));
            v /= BASE;
        }
        return sb.reverse().toString();
    }
}
