# SmartFlow — Error Spec

> 버전: 1.0.0
> 작성일: 2026-04-18

---

## 1. 에러 응답 구조

### REST API
```json
{
  "success": false,
  "error": {
    "code": "AUTH_001",
    "message": "인증이 필요합니다."
  }
}
```

### WebSocket (STOMP)
```json
{
  "type": "ERROR",
  "content": "AI 응답 중 오류가 발생했습니다. 다시 시도해 주세요.",
  "messageId": null
}
```

---

## 2. 에러 코드 전체 목록

### AUTH — 인증/인가

| 코드 | HTTP | 메시지 | 발생 상황 |
|------|------|--------|----------|
| `AUTH_001` | 401 | 인증이 필요합니다. | Authorization 헤더 없음 |
| `AUTH_002` | 400 | 유효하지 않은 인가 코드입니다. | Google OAuth 코드 만료/불일치 |
| `AUTH_003` | 502 | Google 인증 서버 오류입니다. | Google API 호출 실패 |
| `AUTH_004` | 401 | 토큰이 만료되었습니다. | Access Token 만료 |
| `AUTH_005` | 401 | 유효하지 않은 토큰입니다. | JWT 서명 불일치, 변조 |
| `AUTH_006` | 401 | Refresh Token이 만료되었습니다. | Refresh Token 7일 초과 |
| `AUTH_007` | 401 | Refresh Token이 일치하지 않습니다. | Redis에 저장된 값과 불일치 |

---

### CONV — 대화

| 코드 | HTTP | 메시지 | 발생 상황 |
|------|------|--------|----------|
| `CONV_001` | 404 | 대화를 찾을 수 없습니다. | 존재하지 않는 conversationId |
| `CONV_002` | 403 | 해당 대화에 접근할 권한이 없습니다. | 다른 사용자의 대화 접근 |
| `CONV_003` | 400 | 대화 제목은 1자 이상 200자 이하여야 합니다. | 제목 길이 위반 |

---

### MSG — 메시지

| 코드 | HTTP/WS | 메시지 | 발생 상황 |
|------|---------|--------|----------|
| `MSG_001` | WS ERROR | 메시지 내용을 입력해 주세요. | 빈 메시지 전송 |
| `MSG_002` | WS ERROR | 메시지는 5000자 이하여야 합니다. | 길이 초과 |
| `MSG_003` | WS ERROR | 현재 AI가 응답 중입니다. | 스트리밍 중 중복 전송 시도 |

---

### AI — Claude API

| 코드 | HTTP/WS | 메시지 | 발생 상황 |
|------|---------|--------|----------|
| `AI_001` | WS ERROR | AI 응답 중 오류가 발생했습니다. 다시 시도해 주세요. | Anthropic API 일반 오류 |
| `AI_002` | WS ERROR | AI 응답 시간이 초과되었습니다. 다시 시도해 주세요. | 30초 타임아웃 |
| `AI_003` | 502 | AI 서비스가 일시적으로 사용 불가합니다. | Anthropic API 5xx |
| `AI_004` | 429 | AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요. | Rate Limit |

---

### MCP — Google MCP

| 코드 | HTTP/WS | 메시지 | 발생 상황 |
|------|---------|--------|----------|
| `MCP_001` | WS ERROR | Google 서비스 연결에 실패했습니다. | MCP 서버 연결 오류 |
| `MCP_002` | WS ERROR | Gmail 접근 권한이 없습니다. Google 계정을 재연결해 주세요. | Gmail 권한 만료/미부여 |
| `MCP_003` | WS ERROR | Google Calendar 접근 권한이 없습니다. | Calendar 권한 만료/미부여 |
| `MCP_004` | WS ERROR | Google Drive 접근 권한이 없습니다. | Drive 권한 만료/미부여 |
| `MCP_005` | WS ERROR | Google 서비스 응답 시간이 초과되었습니다. | MCP 10초 타임아웃 |

---

### VAL — 유효성 검증

| 코드 | HTTP | 메시지 | 발생 상황 |
|------|------|--------|----------|
| `VAL_001` | 400 | 필수 입력값이 누락되었습니다. ({fieldName}) | @NotBlank, @NotNull 위반 |
| `VAL_002` | 400 | 입력값이 유효하지 않습니다. ({fieldName}) | @Valid 기타 위반 |

---

### SYS — 시스템

| 코드 | HTTP | 메시지 | 발생 상황 |
|------|------|--------|----------|
| `SYS_001` | 500 | 서버 내부 오류가 발생했습니다. | 예상치 못한 RuntimeException |
| `SYS_002` | 503 | 서비스를 일시적으로 사용할 수 없습니다. | DB 연결 실패 등 |

---

## 3. 백엔드 구현

### 3.1 ErrorCode.java (Enum)
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // AUTH
    AUTH_001(HttpStatus.UNAUTHORIZED,  "인증이 필요합니다."),
    AUTH_002(HttpStatus.BAD_REQUEST,   "유효하지 않은 인가 코드입니다."),
    AUTH_003(HttpStatus.BAD_GATEWAY,   "Google 인증 서버 오류입니다."),
    AUTH_004(HttpStatus.UNAUTHORIZED,  "토큰이 만료되었습니다."),
    AUTH_005(HttpStatus.UNAUTHORIZED,  "유효하지 않은 토큰입니다."),
    AUTH_006(HttpStatus.UNAUTHORIZED,  "Refresh Token이 만료되었습니다."),
    AUTH_007(HttpStatus.UNAUTHORIZED,  "Refresh Token이 일치하지 않습니다."),

    // CONV
    CONV_001(HttpStatus.NOT_FOUND,     "대화를 찾을 수 없습니다."),
    CONV_002(HttpStatus.FORBIDDEN,     "해당 대화에 접근할 권한이 없습니다."),
    CONV_003(HttpStatus.BAD_REQUEST,   "대화 제목은 1자 이상 200자 이하여야 합니다."),

    // MSG
    MSG_001(HttpStatus.BAD_REQUEST,    "메시지 내용을 입력해 주세요."),
    MSG_002(HttpStatus.BAD_REQUEST,    "메시지는 5000자 이하여야 합니다."),
    MSG_003(HttpStatus.CONFLICT,       "현재 AI가 응답 중입니다."),

    // AI
    AI_001(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 중 오류가 발생했습니다. 다시 시도해 주세요."),
    AI_002(HttpStatus.GATEWAY_TIMEOUT,       "AI 응답 시간이 초과되었습니다. 다시 시도해 주세요."),
    AI_003(HttpStatus.BAD_GATEWAY,           "AI 서비스가 일시적으로 사용 불가합니다."),
    AI_004(HttpStatus.TOO_MANY_REQUESTS,     "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // MCP
    MCP_001(HttpStatus.BAD_GATEWAY,          "Google 서비스 연결에 실패했습니다."),
    MCP_002(HttpStatus.FORBIDDEN,            "Gmail 접근 권한이 없습니다. Google 계정을 재연결해 주세요."),
    MCP_003(HttpStatus.FORBIDDEN,            "Google Calendar 접근 권한이 없습니다."),
    MCP_004(HttpStatus.FORBIDDEN,            "Google Drive 접근 권한이 없습니다."),
    MCP_005(HttpStatus.GATEWAY_TIMEOUT,      "Google 서비스 응답 시간이 초과되었습니다."),

    // VAL
    VAL_001(HttpStatus.BAD_REQUEST,   "필수 입력값이 누락되었습니다."),
    VAL_002(HttpStatus.BAD_REQUEST,   "입력값이 유효하지 않습니다."),

    // SYS
    SYS_001(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    SYS_002(HttpStatus.SERVICE_UNAVAILABLE,   "서비스를 일시적으로 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
```

### 3.2 SmartflowException.java
```java
@Getter
public class SmartflowException extends RuntimeException {

    private final ErrorCode errorCode;

    public SmartflowException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 3.3 ErrorResponse.java
```java
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
```

### 3.4 GlobalExceptionHandler.java
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // SmartflowException: 의도된 비즈니스 예외
    @ExceptionHandler(SmartflowException.class)
    public ResponseEntity<ErrorResponse> handleSmartflowException(SmartflowException e) {
        log.warn("SmartflowException: {}", e.getErrorCode());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ErrorResponse.of(e.getErrorCode()));
    }

    // @Valid 유효성 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e) {
        String fieldName = e.getBindingResult().getFieldErrors()
            .get(0).getField();
        ErrorResponse body = ErrorResponse.of(ErrorCode.VAL_001);
        // 필드명 포함하여 메시지 재조합 가능
        return ResponseEntity.badRequest().body(body);
    }

    // 그 외 예상치 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(ErrorCode.SYS_001));
    }
}
```

### 3.5 WebSocket 에러 전송
```java
// ClaudeService.java — 스트리밍 중 예외 발생 시
private void sendError(String conversationId, String errorMessage) {
    StreamChunkResponse errorChunk = StreamChunkResponse.builder()
        .type("ERROR")
        .content(errorMessage)
        .messageId(null)
        .build();
    messageBroker.broadcast(conversationId, errorChunk);
}

// 사용 예
try {
    claudeApiClient.streamResponse(request, chunk -> {
        messageBroker.broadcast(conversationId, chunk);
    });
} catch (AnthropicTimeoutException e) {
    sendError(conversationId, ErrorCode.AI_002.getMessage());
} catch (AnthropicRateLimitException e) {
    sendError(conversationId, ErrorCode.AI_004.getMessage());
} catch (McpConnectionException e) {
    sendError(conversationId, ErrorCode.MCP_001.getMessage());
} catch (Exception e) {
    log.error("Streaming error", e);
    sendError(conversationId, ErrorCode.AI_001.getMessage());
}
```

---

## 4. 프론트엔드 에러 처리

### 4.1 Axios 인터셉터 (api/axios.js)
```javascript
// 응답 인터셉터
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { status, data } = error.response ?? {}
    const errorCode = data?.error?.code

    // 401: 토큰 만료 처리
    if (status === 401) {
      if (errorCode === 'AUTH_004') {
        // Access Token 만료 → Refresh 시도
        try {
          await authStore.refreshToken()
          return axiosInstance(error.config) // 원래 요청 재시도
        } catch {
          authStore.logout()
          router.push('/login')
        }
      } else {
        // Refresh Token 만료 등 → 로그인 페이지
        authStore.logout()
        router.push('/login')
      }
    }

    // 403: 권한 없음 → 토스트 알림
    if (status === 403) {
      useToast().error(data?.error?.message ?? '접근 권한이 없습니다.')
    }

    // 그 외: 에러 그대로 throw
    return Promise.reject(error)
  }
)
```

### 4.2 WebSocket 에러 처리 (composables/useWebSocket.js)
```javascript
stompClient.subscribe(`/topic/chat/${conversationId}`, (frame) => {
  const chunk = JSON.parse(frame.body)

  switch (chunk.type) {
    case 'CHUNK':
      messageStore.appendChunk(chunk.content)
      break

    case 'MCP_CALL':
      messageStore.setMcpCall(chunk.content)
      break

    case 'DONE':
      messageStore.finishStreaming(chunk.messageId)
      break

    case 'ERROR':
      messageStore.handleError(chunk.content)
      useToast().error(chunk.content)      // 토스트 알림
      break
  }
})

// WebSocket 연결 끊김 처리
stompClient.onDisconnect = () => {
  connectionStatus.value = 'disconnected'
  // 3초 후 자동 재연결 (최대 3회)
  scheduleReconnect()
}
```

### 4.3 에러별 UI 처리 방침

| 에러 코드 | UI 처리 |
|----------|--------|
| `AUTH_004` | 자동 토큰 갱신 후 재요청 (사용자 모름) |
| `AUTH_006`, `AUTH_007` | 로그인 페이지 리다이렉트 |
| `CONV_001` | "대화를 찾을 수 없습니다" 페이지 + 새 대화 버튼 |
| `CONV_002` | "접근 권한이 없습니다" 토스트 + 대시보드 이동 |
| `AI_001`, `AI_002` | 에러 메시지 버블 + "다시 시도" 버튼 |
| `AI_004` | "잠시 후 다시 시도해 주세요" 토스트 + 입력창 일시 비활성 |
| `MCP_002~004` | "Google 계정을 재연결해 주세요" 배너 (재로그인 유도) |
| `MCP_005` | 에러 메시지 버블 + "다시 시도" 버튼 |
| `SYS_001` | "서버 오류가 발생했습니다" 토스트 |
| WS 연결 끊김 | 상단 배너 "연결이 끊겼습니다. 재연결 중..." |

---

## 5. 에러 메시지 UI 컴포넌트

### 에러 메시지 버블 (채팅 내)
```
┌────────────────────────────────┐
│ [SF]  ⚠ AI 응답 중 오류가     │  ← 빨간 테두리 버블
│       발생했습니다.            │
│       [다시 시도]              │  ← 버튼 클릭 시 동일 메시지 재전송
└────────────────────────────────┘
```

### 토스트 알림 (상단 오른쪽)
```
┌─────────────────────────────────┐
│ ⚠ Gmail 접근 권한이 없습니다.  │  ← 3초 자동 닫힘
│   Google 계정을 재연결해 주세요 │
└─────────────────────────────────┘
```

### 연결 끊김 배너 (상단 전체)
```
┌─────────────────────────────────────────────┐
│ ● 연결이 끊겼습니다. 재연결 중... (2/3)      │
└─────────────────────────────────────────────┘
```
