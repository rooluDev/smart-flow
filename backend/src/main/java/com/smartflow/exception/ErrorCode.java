package com.smartflow.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    AUTH_001(HttpStatus.UNAUTHORIZED,             "인증이 필요합니다."),
    AUTH_002(HttpStatus.BAD_REQUEST,              "유효하지 않은 인가 코드입니다."),
    AUTH_003(HttpStatus.BAD_GATEWAY,              "Google 인증 서버 오류입니다."),
    AUTH_004(HttpStatus.UNAUTHORIZED,             "토큰이 만료되었습니다."),
    AUTH_005(HttpStatus.UNAUTHORIZED,             "유효하지 않은 토큰입니다."),
    AUTH_006(HttpStatus.UNAUTHORIZED,             "Refresh Token이 만료되었습니다."),
    AUTH_007(HttpStatus.UNAUTHORIZED,             "Refresh Token이 일치하지 않습니다."),

    CONV_001(HttpStatus.NOT_FOUND,                "대화를 찾을 수 없습니다."),
    CONV_002(HttpStatus.FORBIDDEN,                "해당 대화에 접근할 권한이 없습니다."),
    CONV_003(HttpStatus.BAD_REQUEST,              "대화 제목은 1자 이상 200자 이하여야 합니다."),

    MSG_001(HttpStatus.BAD_REQUEST,               "메시지 내용을 입력해 주세요."),
    MSG_002(HttpStatus.BAD_REQUEST,               "메시지는 5000자 이하여야 합니다."),
    MSG_003(HttpStatus.CONFLICT,                  "현재 AI가 응답 중입니다."),

    AI_001(HttpStatus.INTERNAL_SERVER_ERROR,      "AI 응답 중 오류가 발생했습니다. 다시 시도해 주세요."),
    AI_002(HttpStatus.GATEWAY_TIMEOUT,            "AI 응답 시간이 초과되었습니다. 다시 시도해 주세요."),
    AI_003(HttpStatus.BAD_GATEWAY,                "AI 서비스가 일시적으로 사용 불가합니다."),
    AI_004(HttpStatus.TOO_MANY_REQUESTS,          "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    MCP_001(HttpStatus.BAD_GATEWAY,               "Google 서비스 연결에 실패했습니다."),
    MCP_002(HttpStatus.FORBIDDEN,                 "Gmail 접근 권한이 없습니다. Google 계정을 재연결해 주세요."),
    MCP_003(HttpStatus.FORBIDDEN,                 "Google Calendar 접근 권한이 없습니다."),
    MCP_004(HttpStatus.FORBIDDEN,                 "Google Drive 접근 권한이 없습니다."),
    MCP_005(HttpStatus.GATEWAY_TIMEOUT,           "Google 서비스 응답 시간이 초과되었습니다."),

    VAL_001(HttpStatus.BAD_REQUEST,               "필수 입력값이 누락되었습니다."),
    VAL_002(HttpStatus.BAD_REQUEST,               "입력값이 유효하지 않습니다."),

    SYS_001(HttpStatus.INTERNAL_SERVER_ERROR,     "서버 내부 오류가 발생했습니다."),
    SYS_002(HttpStatus.SERVICE_UNAVAILABLE,       "서비스를 일시적으로 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
