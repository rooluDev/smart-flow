package com.smartflow.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success = false;
    private final ErrorDetail error;

    @Getter
    @Builder
    public static class ErrorDetail {
        private final String code;
        private final String message;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .error(ErrorDetail.builder()
                        .code(errorCode.name())
                        .message(errorCode.getMessage())
                        .build())
                .build();
    }
}
