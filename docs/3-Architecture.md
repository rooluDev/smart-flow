# SmartFlow — Architecture

> 버전: 1.0.0
> 작성일: 2026-04-18

---

## 1. 시스템 전체 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        Client                               │
│  Vue.js 3 SPA  (Vite + Pinia + Vue Router + Axios)         │
│  - /login   - /dashboard   - /chat   - /chat/:id           │
└──────────────────────┬──────────────────────────────────────┘
                       │  REST API (HTTP/HTTPS)
                       │  WebSocket (STOMP over SockJS)
┌──────────────────────▼──────────────────────────────────────┐
│                   Spring Boot 3.3                           │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Web Layer  │  │Service Layer │  │   Data Layer     │  │
│  │  REST API   │  │  AI Service  │  │  JPA Repository  │  │
│  │  WebSocket  │  │  MCP Service │  │  MyBatis Mapper  │  │
│  │  Handler    │  │  Auth Service│  │                  │  │
│  └─────────────┘  └──────────────┘  └──────────────────┘  │
│                                                             │
│  Spring Security (JWT Filter) + CORS Config                │
└───────┬──────────────┬────────────────────┬────────────────┘
        │              │                    │
        ▼              ▼                    ▼
   ┌─────────┐  ┌───────────┐     ┌────────────────────┐
   │  MySQL  │  │   Redis   │     │  Anthropic API     │
   │  8.0    │  │   7.x     │     │  claude-sonnet-4-6 │
   │         │  │  Token    │     │  + tools(Tool Use)     │
   │  users  │  │  Session  │     │       │            │
   │  convs  │  │  Cache    │     │  ┌────┴──────────┐ │
   │  msgs   │  └───────────┘     │  │Gmail Calendar │ │
   └─────────┘                   │  │      Drive    │ │
                                  │  └───────────────┘ │
                                  └────────────────────┘
```

---

## 2. 백엔드 패키지 구조

```
src/main/java/com/smartflow/
│
├── SmartflowApplication.java
│
├── config/
│   ├── SecurityConfig.java          # Spring Security, JWT 필터 등록
│   ├── WebSocketConfig.java         # STOMP 엔드포인트, 브로커 설정
│   ├── RedisConfig.java             # RedisTemplate, Cache 설정
│   ├── CorsConfig.java              # CORS 허용 Origin 설정
│   └── ClaudeApiConfig.java         # Anthropic API 클라이언트 빈
│
├── controller/
│   ├── AuthController.java          # POST /api/auth/google, /refresh, /logout
│   ├── ConversationController.java  # GET/POST/DELETE /api/conversations
│   └── DashboardController.java     # GET /api/dashboard
│
├── websocket/
│   ├── ChatWebSocketHandler.java    # STOMP /app/chat.send 구독
│   └── ChatMessageBroker.java      # 스트리밍 토큰 → 클라이언트 발행
│
├── service/
│   ├── auth/
│   │   ├── AuthService.java         # OAuth 코드 교환, JWT 발급
│   │   ├── JwtService.java          # Token 생성/검증/갱신
│   │   └── GoogleOAuthClient.java   # Google OAuth2 API 클라이언트
│   │
│   ├── ai/
│   │   ├── ClaudeService.java       # Anthropic API 호출, Tool Use 루프, 스트리밍 처리
│   │   ├── GoogleToolExecutor.java  # Google API 직접 호출 (Calendar/Gmail/Drive)
│   │   └── ConversationHistory.java # 대화 히스토리 컨텍스트 관리
│   │
│   ├── mcp/
│   │   └── GoogleMcpTokenManager.java # Google OAuth Token 관리 (Redis 캐싱 + 자동 갱신)
│   │
│   ├── ConversationService.java     # 대화 CRUD
│   ├── MessageService.java          # 메시지 저장/조회
│   └── DashboardService.java        # 대시보드 데이터 집계
│
├── repository/
│   ├── UserRepository.java          # JPA: users 테이블
│   ├── ConversationRepository.java  # JPA: conversations 테이블
│   └── MessageRepository.java       # JPA: messages 테이블
│
├── mapper/
│   └── DashboardMapper.java         # MyBatis: 복잡한 대시보드 쿼리
│
├── domain/
│   ├── User.java                    # @Entity
│   ├── Conversation.java            # @Entity
│   └── Message.java                 # @Entity (role: USER | ASSISTANT)
│
├── dto/
│   ├── request/
│   │   ├── ChatMessageRequest.java
│   │   └── ConversationCreateRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── ConversationResponse.java
│       ├── MessageResponse.java
│       ├── DashboardResponse.java
│       └── StreamChunkResponse.java  # WebSocket 스트리밍 청크
│
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── SmartflowException.java      # 공통 예외 베이스
│   ├── ErrorCode.java               # 에러 코드 열거형
│   └── ErrorResponse.java           # 에러 응답 DTO
│
├── security/
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter
│   └── UserPrincipal.java           # SecurityContext 사용자 정보
│
└── util/
    ├── DateTimeUtil.java
    └── StringUtil.java
```

---

## 3. 프론트엔드 구조

```
src/
│
├── main.js                          # 앱 진입점, 플러그인 등록
├── App.vue                          # 루트 컴포넌트, RouterView
│
├── router/
│   └── index.js                     # 라우트 정의 + 네비게이션 가드
│
├── stores/
│   ├── auth.js                      # Pinia: 인증 상태, 토큰 관리
│   ├── conversation.js              # Pinia: 대화 목록, 현재 대화
│   ├── message.js                   # Pinia: 메시지 목록, 스트리밍 상태
│   └── dashboard.js                 # Pinia: 대시보드 데이터
│
├── composables/
│   ├── useWebSocket.js              # STOMP 연결/구독/발행
│   ├── useStreamingMessage.js       # 스트리밍 토큰 누적 처리
│   └── useAuth.js                   # 로그인/로그아웃/토큰 갱신
│
├── api/
│   ├── axios.js                     # Axios 인스턴스 + 인터셉터
│   ├── auth.js                      # 인증 API
│   ├── conversation.js              # 대화 API
│   └── dashboard.js                 # 대시보드 API
│
├── views/
│   ├── LoginView.vue                # /login
│   ├── DashboardView.vue            # /dashboard
│   └── ChatView.vue                 # /chat, /chat/:id
│
├── components/
│   ├── layout/
│   │   ├── AppSidebar.vue           # 대화 목록 사이드바
│   │   └── AppHeader.vue            # 상단 네비게이션
│   │
│   ├── chat/
│   │   ├── MessageList.vue          # 메시지 스크롤 목록
│   │   ├── MessageItem.vue          # 단일 메시지 (User/AI 구분)
│   │   ├── MessageInput.vue         # 입력창 + 전송 버튼
│   │   └── StreamingCursor.vue      # 스트리밍 중 커서 표시
│   │
│   └── dashboard/
│       ├── EmailWidget.vue          # 미읽은 이메일 위젯
│       ├── CalendarWidget.vue       # 오늘 일정 위젯
│       └── DriveWidget.vue          # 최근 파일 위젯
│
└── assets/
    └── styles/
        └── main.css                 # 전역 스타일
```

---

## 4. WebSocket 스트리밍 데이터 흐름

### 4.1 연결 설정
```
[Vue.js] SockJS 연결 → ws://localhost:8080/ws-smartflow
→ STOMP 핸드셰이크
→ /topic/chat/{conversationId} 구독
```

### 4.2 메시지 송수신
```
[Vue.js]  STOMP /app/chat.send
          payload: { conversationId, content, accessToken }
                │
                ▼
[Spring]  ChatWebSocketHandler.handleMessage()
          → MessageService.save(USER 메시지)
          → ClaudeService.streamResponse()
                │
                ▼
[Anthropic API]  SSE 스트리밍 응답
                │  (토큰 단위)
                ▼
[Spring]  ChatMessageBroker.broadcast()
          → /topic/chat/{conversationId}로 청크 발행
          payload: { type: "CHUNK"|"DONE"|"ERROR", content }
                │
                ▼
[Vue.js]  useStreamingMessage.appendChunk()
          → 화면에 실시간 렌더링
```

### 4.3 스트리밍 청크 타입
| type | 의미 | content |
|------|------|---------|
| `CHUNK` | 토큰 조각 | 텍스트 조각 |
| `MCP_CALL` | Google 도구 호출 중 | 도구 표시명 (예: "Google Calendar") |
| `DONE` | 응답 완료 | `null` |
| `ERROR` | 오류 발생 | 에러 메시지 |

---

## 5. 인증 아키텍처

### JWT 구조
```
Access Token
├── 유효기간: 1시간
├── Payload: { userId, email, iat, exp }
└── 전달: Authorization: Bearer {token}

Refresh Token
├── 유효기간: 7일
├── 저장소: Redis (key: smartflow:refresh:{userId})
└── 전달: 응답 Body (POST /api/auth/refresh 요청 시 Body로 전달)
```

### 요청 인증 흐름
```
[모든 API 요청]
      │
      ▼
JwtAuthenticationFilter
      │
      ├── Access Token 유효 → SecurityContext 설정 → 컨트롤러 진입
      │
      └── Access Token 만료 → 401 반환
              │
              ▼ (프론트엔드가 처리)
         POST /api/auth/refresh (Refresh Token 전달)
              │
              ├── Refresh Token 유효 → 새 Access Token 발급
              └── Refresh Token 만료 → 401 → 로그인 화면
```

---

## 6. Tool Use 연동 아키텍처

### Claude API 호출 구조 (Tool Use 루프)
```java
// ClaudeService.java — executeStreamRound()
Map<String, Object> requestBody = Map.of(
    "model", "claude-sonnet-4-6",
    "max_tokens", 4096,
    "system", buildSystemPrompt(),   // 오늘 날짜 포함
    "messages", messages,
    "stream", true,
    "tools", List.of(
        Map.of("name", "get_calendar_events", ...),
        Map.of("name", "get_gmail_messages",  ...),
        Map.of("name", "get_drive_files",     ...),
        Map.of("name", "send_gmail",          ...)
    )
);

// Claude → stop_reason: "tool_use"
// → GoogleToolExecutor.execute(toolName, input, accessToken)
// → Google REST API 직접 호출
// → tool_result 포함하여 재요청 → 최종 응답 스트리밍
```

### Google API 인증 흐름
```
사용자 Google OAuth 로그인
      │ (gmail, calendar, drive scope 포함)
      ▼
Google Access Token + Refresh Token 발급
      │
      ▼
백엔드 DB 저장 (AES-256 암호화) + Redis 캐시
(key: smartflow:google_token:{userId})
      │
      ▼
Tool Use 실행 시 GoogleMcpTokenManager.getAccessToken()
→ Redis 캐시 조회 → 만료 시 Refresh Token으로 자동 갱신
      │
      ▼
GoogleToolExecutor → Google REST API 호출 시 Bearer 헤더에 포함
```

### 지원 도구 목록
| 도구명 | Google API | 설명 |
|--------|-----------|------|
| `get_calendar_events` | Calendar v3 events.list | 특정 기간 일정 조회 (UTC 시간 사용) |
| `get_gmail_messages` | Gmail v1 messages.list/get | 메일 검색 및 메타데이터 조회 |
| `get_drive_files` | Drive v3 files.list | 최근 수정 파일 목록 조회 |
| `send_gmail` | Gmail v1 messages.send | 이메일 발송 |

---

## 7. 데이터베이스 접근 전략

| 용도 | 기술 | 이유 |
|------|------|------|
| 사용자, 대화, 메시지 CRUD | Spring Data JPA | 단순 CRUD에 적합 |
| 대시보드 집계 쿼리 | MyBatis | 복잡한 JOIN, 집계에 유연 |
| Token 저장 / 세션 | Redis | TTL 기반 자동 만료 |
| 빠른 대화 목록 캐시 | Redis | 반복 조회 성능 개선 |

---

## 8. 인프라 구성 (Docker Compose)

```yaml
# docker-compose.yml (로컬 개발용 — MySQL + Redis만 컨테이너로 실행)
# Spring Boot, Vue.js는 IDE/터미널에서 직접 실행
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_DATABASE: smartflow
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

> 운영 배포용은 `docker-compose.prod.yml`을 사용한다. (backend + frontend 컨테이너 포함)

---

## 9. 주요 설계 결정

| 결정 | 선택 | 대안 | 이유 |
|------|------|------|------|
| 실시간 통신 | WebSocket (STOMP) | SSE | 양방향 통신 가능, Spring 지원 좋음 |
| 상태관리 | Pinia | Vuex | Vue 3 공식, 더 간결한 API |
| ORM 전략 | JPA + MyBatis 혼용 | JPA만 사용 | 복잡 쿼리는 MyBatis가 명확 |
| Token 저장 | Redis | DB 저장 | TTL 자동 만료, 빠른 조회 |
| Google 연동 방식 | Tool Use + 백엔드 직접 API 호출 | 원격 MCP 서버 | 실제 존재하는 인프라 사용, 토큰 직접 제어 가능 |
| AI 스트리밍 | SSE → WebSocket 변환 | SSE 직접 프론트 전달 | 백엔드 미들웨어 처리 일원화 |
