package com.aiprototype.urlshortener.exception;

public class UrlAlreadyExistsException extends RuntimeException {
    public UrlAlreadyExistsException(String code) {
        super("Short code already in use: " + code);
    }
}
