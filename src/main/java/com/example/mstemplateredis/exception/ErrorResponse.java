package com.example.mstemplateredis.exception;

import java.time.Instant;

public record ErrorResponse(
        String errorCode,
        String message,
        Instant timestamp
) {}