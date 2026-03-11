package com.company.platform.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String code;
    private final String message;
    private final String traceId;
    @Builder.Default
    private final Instant timestamp = Instant.now();
    private final Object details;
}
