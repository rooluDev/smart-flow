package com.smartflow.exception;

import lombok.Getter;

@Getter
public class SmartflowException extends RuntimeException {

    private final ErrorCode errorCode;

    public SmartflowException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
