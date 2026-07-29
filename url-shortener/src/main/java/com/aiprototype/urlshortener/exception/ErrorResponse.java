package com.aiprototype.urlshortener.exception;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private String timestamp;
    @Builder.Default
    private List<String> details = List.of();
}
