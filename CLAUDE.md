# CLAUDE.md — SmartFlow

> Claude Code가 이 프로젝트를 개발할 때 반드시 읽고 따르는 지침서.
> 모든 코드 생성, 파일 수정, 명령 실행 전에 이 문서를 참조한다.

---

## 1. 프로젝트 개요

**SmartFlow** — Gmail · Google Calendar · Google Drive를 Claude AI가 자연어로 통합 제어하는 개인 업무 자동화 플랫폼.

```
smartflow/
├── CLAUDE.md              ← 현재 파일 (프로젝트 루트 — Claude Code 자동 인식)
├── plan.md                ← 개발 진행 체크리스트 (수시 업데이트)
├── backend/               # Spring Boot 3.3 / Java 17
├── frontend/              # Vue.js 3 / Vite
├── docker-compose.yml
└── docs/
    ├── 1-PRD.md
    ├── 2-UserFlow.md
    ├── 3-Architecture.md
    ├── 4-DBSchema.md
    ├── 5-APISpec.md
    ├── 6-UISpec.md
    ├── 7-ErrorSpec.md
    └── 8-EnvSpec.md
```

---

## 2. 기술 스택

### 백엔드
| 항목 | 값 |
|------|-----|
| Language | Java 17 |
| Framework | Spring Boot 3.3.x |
| Build | Gradle |
| ORM | Spring Data JPA + MyBatis |
| DB | MySQL 8.0 |
| Cache | Redis 7.x |
| Realtime | WebSocket (STOMP over SockJS) |
| AI | Anthropic Claude API (`claude-sonnet-4-6`) |
| Auth | Spring Security + JWT |

### 프론트엔드
| 항목 | 값 |
|------|-----|
| Framework | Vue.js 3 |
| Build | Vite |
| State | Pinia |
| Router | Vue Router 4 |
| HTTP | Axios |
| WebSocket | @stomp/stompjs + sockjs-client |

---

## 3. 개발 환경 시작

```bash
# 인프라 (MySQL + Redis)
docker-compose up -d

# 백엔드
cd backend
cp .env.local.sample .env.local   # 값 입력 후
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드
cd frontend
npm install
npm run dev
```

**필수 환경 변수** (`backend/.env.local`):
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` — Google Cloud Console에서 발급
- `ANTHROPIC_API_KEY` — Anthropic Console에서 발급
- `JWT_SECRET` — 최소 32자 랜덤 문자열
- `ENCRYPTION_SECRET_KEY` — 32자 AES-256 키

상세 내용 → `docs/8-EnvSpec.md`

---

## 4. 패키지 / 디렉터리 구조

### 백엔드 (`backend/src/main/java/com/smartflow/`)
```
config/          SecurityConfig, WebSocketConfig, RedisConfig, CorsConfig, ClaudeApiConfig
controller/      AuthController, ConversationController, DashboardController
websocket/       ChatWebSocketHandler, ChatMessageBroker
service/
  auth/          AuthService, JwtService, GoogleOAuthClient
  ai/            ClaudeService, GoogleToolExecutor, ConversationHistory
  mcp/           GoogleMcpTokenManager (Google OAuth 토큰 관리)
  ConversationService, MessageService, DashboardService
repository/      UserRepository, ConversationRepository, MessageRepository
mapper/          DashboardMapper (MyBatis)
domain/          User, Conversation, Message
dto/
  request/       ChatMessageRequest, ConversationCreateRequest
  response/      AuthResponse, ConversationResponse, MessageResponse,
                 DashboardResponse, StreamChunkResponse
exception/       GlobalExceptionHandler, SmartflowException, ErrorCode, ErrorResponse
security/        JwtAuthenticationFilter, UserPrincipal
util/            DateTimeUtil, StringUtil
```

### 프론트엔드 (`frontend/src/`)
```
router/          index.js (라우트 정의 + 네비게이션 가드)
stores/          auth.js, conversation.js, message.js, dashboard.js
composables/     useWebSocket.js, useStreamingMessage.js, useAuth.js
api/             axios.js, auth.js, conversation.js, dashboard.js
views/           LoginView.vue, DashboardView.vue, ChatView.vue
components/
  layout/        AppSidebar.vue, AppHeader.vue
  chat/          MessageList.vue, MessageItem.vue, MessageInput.vue, StreamingCursor.vue
  dashboard/     EmailWidget.vue, CalendarWidget.vue, DriveWidget.vue
```

---

## 5. 핵심 구현 패턴

### 5.1 Anthropic API + Tool Use 호출 구조

```java
// ClaudeService.java — 핵심 호출 패턴 (Tool Use)
Map<String, Object> requestBody = Map.of(
    "model", "claude-sonnet-4-6",
    "max_tokens", 4096,
    "system", SYSTEM_PROMPT,          // 오늘 날짜 포함
    "messages", messages,
    "stream", true,
    "tools", List.of(
        Map.of("name", "get_calendar_events", "description", "...", "input_schema", ...),
        Map.of("name", "get_gmail_messages",  "description", "...", "input_schema", ...),
        Map.of("name", "get_drive_files",     "description", "...", "input_schema", ...),
        Map.of("name", "send_gmail",          "description", "...", "input_schema", ...)
    )
);
```

**Tool Use 루프 (ClaudeService.executeStreamRound):**
1. Anthropic API 스트리밍 요청 (`tools` 파라미터 포함)
2. Claude가 도구 필요 시 → `stop_reason: "tool_use"` + `content_block` 반환
3. 백엔드가 `GoogleToolExecutor.execute(toolName, input, accessToken)` 직접 실행
4. Google API 결과를 `tool_result`로 포함하여 재요청
5. Claude가 최종 답변 스트리밍 → `stop_reason: "end_turn"` → DONE 브로드캐스트

**지원 도구 (GoogleToolExecutor):**
- `get_calendar_events(start_date, end_date)` → Google Calendar API 직접 호출
- `get_gmail_messages(query, max_results)` → Gmail API 직접 호출
- `get_drive_files(max_results)` → Google Drive API 직접 호출
- `send_gmail(to, subject, body)` → Gmail API 직접 호출

- Google OAuth 토큰은 `GoogleMcpTokenManager`로 관리 (Redis 캐싱 + 자동 갱신)
- Calendar API 시간 파라미터는 `Instant.toString()`(UTC/Z)으로 변환 — `+09:00`의 URL 인코딩 문제 방지
- 스트리밍 응답은 SSE를 파싱하여 `ChatMessageBroker`로 브로드캐스트

### 5.2 WebSocket 스트리밍 청크 타입

```java
// StreamChunkResponse.java
// type: "CHUNK" | "MCP_CALL" | "DONE" | "ERROR"
// content: 텍스트(CHUNK), 도구 표시명(MCP_CALL, 예: "Google Calendar"), null(DONE), 에러 메시지(ERROR)
// messageId: 저장된 메시지 ID(DONE), null(나머지)
```

```javascript
// useWebSocket.js — 프론트엔드 분기
switch (chunk.type) {
  case 'CHUNK':    messageStore.appendChunk(chunk.content); break;
  case 'MCP_CALL': messageStore.setMcpCall(chunk.content); break;
  case 'DONE':     messageStore.finishStreaming(chunk.messageId); break;
  case 'ERROR':    messageStore.handleError(chunk.content); break;
}
```

### 5.3 JWT 인증 흐름

```
요청 → JwtAuthenticationFilter
  → Authorization: Bearer {token} 추출
  → JwtService.validateToken()
  → SecurityContext에 UserPrincipal 설정
  → 컨트롤러 진입
```

- Access Token 만료(`AUTH_004`) → 프론트엔드 Axios 인터셉터가 자동 갱신
- Refresh Token은 Redis `smartflow:refresh:{userId}`에 TTL 7일로 저장
- WebSocket 연결 시 STOMP 헤더의 `Authorization` 검증

### 5.4 에러 처리 패턴

```java
// 서비스 레이어에서 던지기
throw new SmartflowException(ErrorCode.CONV_001);

// GlobalExceptionHandler에서 잡기
@ExceptionHandler(SmartflowException.class)
public ResponseEntity<ErrorResponse> handle(SmartflowException e) {
    return ResponseEntity.status(e.getErrorCode().getStatus())
                         .body(ErrorResponse.of(e.getErrorCode()));
}
```

```javascript
// 프론트엔드 — API 에러 처리
// 성공: response.data.data 사용
// 실패: response.data.error.code 로 분기
```

### 5.5 Google Token 암호화

```java
// User.java
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "google_access_token", columnDefinition = "TEXT")
private String googleAccessToken;
```

- `EncryptedStringConverter`는 AES-256-GCM으로 저장/복호화
- `ENCRYPTION_SECRET_KEY` 환경 변수 사용

---

## 6. API 규칙

### REST 응답 포맷

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "AUTH_001", "message": "..." } }
```

### 엔드포인트 목록 (요약)
```
POST   /api/auth/google              # Google OAuth 로그인
POST   /api/auth/refresh             # Token 갱신
POST   /api/auth/logout              # 로그아웃
GET    /api/users/me                 # 내 정보
GET    /api/conversations            # 대화 목록 (page, size)
POST   /api/conversations            # 새 대화 생성
GET    /api/conversations/{id}       # 대화 + 메시지
PATCH  /api/conversations/{id}/title # 제목 수정
DELETE /api/conversations/{id}       # 대화 삭제
GET    /api/dashboard                # 대시보드 데이터
WS     /app/chat.send                # 채팅 메시지 전송
WS     /topic/chat/{id}              # 스트리밍 응답 구독
```

전체 스펙 → `docs/5-APISpec.md`

---

## 7. DB 규칙

### 테이블 요약
```
users          (id, google_id, email, name, google_access_token*, google_refresh_token*, ...)
conversations  (id, user_id FK, title, created_at, updated_at)
messages       (id, conversation_id FK, role ENUM, content LONGTEXT, mcp_tools_used JSON, created_at)
```
`*` AES 암호화 저장

### ORM 전략
- 단순 CRUD → Spring Data JPA (`UserRepository`, `ConversationRepository`, `MessageRepository`)
- 복잡한 JOIN/집계 → MyBatis (`DashboardMapper`)

### Redis 키 패턴
```
smartflow:refresh:{userId}         # Refresh Token (TTL 7일)
smartflow:google_token:{userId}    # Google OAuth Token Hash
smartflow:dashboard:{userId}       # 대시보드 캐시 (TTL 5분)
smartflow:conv_list:{userId}       # 대화 목록 캐시 (TTL 10분)
```

캐시 무효화: 대화 생성/삭제 시 `conv_list` 삭제, 로그아웃 시 모든 키 삭제.

전체 스펙 → `docs/4-DBSchema.md`

---

## 8. 에러 코드 규칙

| 접두사 | 범위 | 예시 |
|--------|------|------|
| `AUTH_` | 인증/인가 | AUTH_001 ~ AUTH_007 |
| `CONV_` | 대화 | CONV_001 ~ CONV_003 |
| `MSG_`  | 메시지 | MSG_001 ~ MSG_003 |
| `AI_`   | Claude API | AI_001 ~ AI_004 |
| `MCP_`  | Google MCP | MCP_001 ~ MCP_005 |
| `VAL_`  | 유효성 검증 | VAL_001 ~ VAL_002 |
| `SYS_`  | 시스템 | SYS_001 ~ SYS_002 |

전체 코드 + 처리 방식 → `docs/7-ErrorSpec.md`

---

## 9. UI 규칙

### 컬러 / 스타일
- 브랜드 액센트: `#1D9E75` (Teal 400) — AI 아바타, 전송 버튼, 활성 대화 강조선
- CSS 변수(`--color-*`, `--border-radius-*`) 사용, 하드코딩 금지
- 다크 모드 자동 대응 필수

### 메시지 버블
```
USER 버블:
  오른쪽 정렬, background: --color-background-secondary
  border-radius: 20px 20px 4px 20px

ASSISTANT 버블:
  왼쪽 정렬, 아바타 [SF], background: --color-background-primary
  border: 0.5px solid --color-border-tertiary
  border-radius: 4px 20px 20px 20px
  MCP 뱃지: 메시지 상단 (예: "Gmail 조회됨", teal 배경)
```

### 스트리밍 커서
```css
.streaming-cursor {
  display: inline-block; width: 2px; height: 16px;
  background: var(--color-text-primary); margin-left: 2px;
  animation: blink 0.8s step-end infinite;
}
```

전체 스펙 → `docs/6-UISpec.md`

---

## 10. 보안 규칙

1. `.env.local`, `application-local.yml` 파일은 절대 Git 커밋 금지
2. Google OAuth Token은 DB에 AES-256 암호화 저장 (`EncryptedStringConverter`)
3. JWT Secret은 최소 32자 이상 랜덤 문자열
4. 로그에 API Key, Token, 비밀번호 절대 출력 금지
5. 모든 API 엔드포인트에 JWT 인증 필수 (공개 경로 제외: `/api/auth/google`, `/api/auth/refresh`)
6. CORS는 `CORS_ALLOWED_ORIGINS` 환경 변수로만 허용 Origin 관리
7. Google OAuth 스코프는 필요한 최소 범위만 요청

---

## 11. 개발 원칙

1. **수직 슬라이스** — 한 기능을 백엔드-프론트엔드 함께 완성한 뒤 다음으로 넘어간다
2. **동작 우선** — 스타일보다 기능이 먼저. 완성 후 UI를 다듬는다
3. **명세 준수** — 새 파일을 만들기 전에 반드시 해당 스펙 문서를 먼저 확인한다
4. **에러 명시** — 모든 예외는 `SmartflowException(ErrorCode.XXX)` 형태로 던진다
5. **캐시 무효화** — 대화 생성/삭제 시 관련 Redis 캐시 즉시 삭제
6. **스트리밍 완결** — AI 응답이 DONE 또는 ERROR로 반드시 종료되어야 한다. 미종료 시 프론트엔드가 입력창을 영구적으로 비활성화하는 문제 발생

---

## 12. 자주 하는 작업 (Quick Reference)

### 새 API 엔드포인트 추가 시
1. `docs/5-APISpec.md` 에서 Request/Response 형식 확인
2. Controller 메서드 작성 (`@GetMapping`, `@PostMapping` 등)
3. Service 로직 구현
4. `ErrorCode` 필요 시 추가
5. 프론트엔드 `api/*.js` 함수 추가

### 새 Google 도구 추가 시
1. `GoogleToolExecutor`에 `execute()` switch 케이스 추가 + 구현 메서드 작성
2. `ClaudeService.buildToolDefinitions()`에 tool 정의 (name, description, input_schema) 추가
3. `ClaudeService.getToolDisplayName()`에 표시 이름 추가
4. 프론트엔드 `MessageItem.vue` MCP 뱃지 표시 확인 (기존 코드 자동 처리)

### 에러 추가 시
1. `ErrorCode.java` enum에 코드 추가
2. `GlobalExceptionHandler`에 특수 처리 필요 시 추가
3. `docs/7-ErrorSpec.md` 업데이트
4. 프론트엔드 `useWebSocket.js` 또는 Axios 인터셉터에 처리 추가

---

## 13. 현재 진행 상태

> M1~M5 구현 완료. M6 (UI 완성 + QA + 배포 준비) 진행 중.

**다음 작업: M6 — UI 완성 + QA + 배포 준비**

M6 잔여 항목:
- [ ] 반응형 레이아웃 최종 점검 (최소 너비 1024px)
- [ ] 대시보드 위젯 에러 상태 UI
- [ ] M1~M5 시나리오 전체 재검증 (QA)
- [ ] GitHub 저장소 생성 및 초기 커밋

---

## 14. 참고 문서 인덱스

| 문서 | 내용 | 경로 |
|------|------|------|
| PRD | 기능 요구사항, 기술 스택, 마일스톤 | `docs/1-PRD.md` |
| User Flow | 인증/채팅/MCP 플로우 다이어그램 | `docs/2-UserFlow.md` |
| Architecture | 시스템 구조, 패키지 트리, WebSocket 흐름 | `docs/3-Architecture.md` |
| DB Schema | 테이블 정의, DDL, Redis 키 구조, JPA Entity | `docs/4-DBSchema.md` |
| API Spec | 전체 엔드포인트, Request/Response 예시 | `docs/5-APISpec.md` |
| UI Spec | 컴포넌트 스펙, 디자인 토큰, Pinia 스토어 | `docs/6-UISpec.md` |
| Error Spec | 에러 코드 전체, 백엔드/프론트엔드 처리 코드 | `docs/7-ErrorSpec.md` |
| Env Spec | 환경 변수 목록, `.env.local` 샘플, Google OAuth 가이드 | `docs/8-EnvSpec.md` |
| plan.md | 마일스톤별 태스크 체크리스트 | 프로젝트 루트 |
| CLAUDE.md | Claude Code 개발 지침 (현재 파일) | 프로젝트 루트 |
