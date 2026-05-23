# SmartFlow — plan.md

> 버전: 1.0.0
> 작성일: 2026-04-18
> 총 예상 기간: 6주

---

## 마일스톤 요약

| 마일스톤 | 내용 | 기간 | 상태 |
|---------|------|------|------|
| M1 | 프로젝트 셋업 + Google OAuth 인증 | 1주 | 구현 완료 |
| M2 | AI 채팅 기본 기능 (WebSocket 스트리밍) | 1주 | 구현 완료 |
| M3 | Gmail Tool Use 연동 | 1주 | 구현 완료 |
| M4 | Google Calendar Tool Use 연동 | 1주 | 구현 완료 |
| M5 | Google Drive Tool Use 연동 + 대시보드 | 1주 | 구현 완료 |
| M6 | UI 완성 + QA + 배포 준비 | 1주 | 진행 중 |

---

## M1. 프로젝트 셋업 + Google OAuth 인증

### 인프라 / 공통
- [x] 프로젝트 디렉터리 구조 생성 (backend/, frontend/, docker-compose.yml)
- [x] `docker-compose.yml` 작성 (MySQL 8.0, Redis 7)
- [x] `.gitignore` 작성 (시크릿 파일 패턴 등록)
- [x] 백엔드 Gradle 프로젝트 초기화 (Spring Boot 3.3, Java 17)
- [x] 프론트엔드 Vite 프로젝트 초기화 (Vue 3, Pinia, Vue Router)
- [x] `application.yml` 기본 구조 작성
- [x] `backend/.env.local` 샘플 파일 작성

### 백엔드
- [x] 의존성 추가: Spring Web, Spring Security, Spring Data JPA, MyBatis, MySQL, Redis, Lombok, Validation
- [x] DB 연결 설정 및 `smartflow` 데이터베이스 생성 확인
- [x] DDL 실행: `users`, `conversations`, `messages` 테이블 생성
- [x] `User` JPA Entity 작성
- [x] `UserRepository` 작성
- [x] `GoogleOAuthClient` 작성
  - Google Authorization URL 생성
  - Authorization Code → Access/Refresh Token 교환
  - UserInfo API 호출 (email, name, profileImageUrl)
- [x] `AuthService` 작성
  - `loginWithGoogle(code, redirectUri)` → User upsert → JWT 발급
  - `refreshAccessToken(refreshToken)` → Redis 조회 → 새 Access Token 발급
  - `logout(userId)` → Redis Refresh Token 삭제
- [x] `JwtService` 작성 (생성, 검증, userId 추출)
- [x] `JwtAuthenticationFilter` 작성 (OncePerRequestFilter)
- [x] `SecurityConfig` 작성 (JWT 필터 등록, 공개 경로 설정)
- [x] `AuthController` 작성
  - `POST /api/auth/google`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
- [x] `GET /api/users/me` 작성
- [x] `GlobalExceptionHandler` 기본 구조 작성
- [x] `ErrorCode` enum 작성 (AUTH 카테고리 우선)
- [x] Redis 연결 설정 및 `RedisConfig` 작성
- [x] `EncryptedStringConverter` 작성 (Google Token AES 암호화)
- [x] CORS 설정 (`CorsConfig`)

### 프론트엔드
- [x] Vue Router 설정 (라우트 정의, 네비게이션 가드)
- [x] Pinia `authStore` 작성 (user, accessToken 상태관리)
- [x] `api/axios.js` 작성 (Axios 인스턴스, JWT 인터셉터, 401 자동 갱신)
- [x] `api/auth.js` 작성 (Google 로그인, Refresh, Logout API 호출)
- [x] `LoginView.vue` 작성 (Google 로그인 버튼, OAuth 콜백 처리)
- [x] Google OAuth 콜백 처리 (`/oauth/callback` 라우트 → code 추출 → API 호출 → 토큰 저장 → 대시보드 이동)
- [x] `useAuth` composable 작성 (로그인/로그아웃/토큰 갱신)
- [x] `AppHeader.vue` 기본 구조 작성 (사용자 이름, 로그아웃 버튼)
- [x] 인증 완료 후 `/dashboard` 리다이렉트 확인

### M1 완료 기준
- [ ] Google 로그인 → 대시보드 리다이렉트 동작
- [ ] Access Token 만료 시 자동 갱신 동작
- [ ] 로그아웃 후 `/login` 이동 확인

---

## M2. AI 채팅 기본 기능 (WebSocket 스트리밍)

### 백엔드
- [x] `Conversation` JPA Entity 작성
- [x] `Message` JPA Entity 작성 (role ENUM, mcp_tools_used JSON)
- [x] `ConversationRepository`, `MessageRepository` 작성
- [x] `WebSocketConfig` 작성 (STOMP 엔드포인트 `/ws-smartflow`, SockJS 활성화)
- [x] `StreamChunkResponse` DTO 작성 (type, content, messageId)
- [x] `ChatWebSocketHandler` 작성
  - `/app/chat.send` 수신
  - JWT 인증 (STOMP 헤더)
  - `MessageService.save(USER 메시지)` 호출
  - `ClaudeService.streamResponse()` 호출
- [x] `ChatMessageBroker` 작성
  - `/topic/chat/{conversationId}` 브로드캐스트
  - CHUNK / DONE / ERROR 타입 발행
- [x] `ClaudeService` 기본 작성
  - Anthropic API HTTP 클라이언트 구성 (WebClient 또는 HttpClient)
  - SSE 스트리밍 응답 파싱
  - 스트리밍 토큰 → `ChatMessageBroker.broadcast()` 호출
  - DONE 시 ASSISTANT 메시지 DB 저장
  - 예외 처리 (타임아웃, API 오류 → ERROR 타입 전송)
- [x] `ConversationHistory` 작성 (최근 N개 메시지를 Anthropic API 형식으로 변환)
- [x] `ConversationService` 작성 (생성, 목록, 단건 조회, 삭제, 제목 수정)
- [x] `MessageService` 작성 (저장, 히스토리 조회)
- [x] `ConversationController` 작성
  - `GET /api/conversations`
  - `POST /api/conversations`
  - `GET /api/conversations/{id}`
  - `PATCH /api/conversations/{id}/title`
  - `DELETE /api/conversations/{id}`
- [x] 대화 제목 자동 생성 (첫 메시지 전송 후 AI로 짧은 제목 생성 → `PATCH /title` 호출)

### 프론트엔드
- [x] Pinia `conversationStore` 작성 (목록, currentId)
- [x] Pinia `messageStore` 작성 (messages, isStreaming, streamingContent, mcpCallName)
- [x] `useWebSocket` composable 작성
  - SockJS + STOMP 연결
  - `/topic/chat/{id}` 구독
  - CHUNK / MCP_CALL / DONE / ERROR 분기 처리
  - 연결 끊김 시 자동 재연결 (최대 3회)
- [x] `useStreamingMessage` composable 작성 (토큰 누적, 완료 처리)
- [x] `api/conversation.js` 작성 (대화 CRUD API)
- [x] `AppSidebar.vue` 작성
  - 대화 목록 렌더링
  - 새 대화 버튼
  - 활성 대화 강조
  - 삭제 버튼 (hover 시 표시)
- [x] `ChatView.vue` 기본 구조 작성 (헤더 + MessageList + MessageInput)
- [x] `MessageList.vue` 작성 (스크롤, 새 메시지 자동 스크롤)
- [x] `MessageItem.vue` 작성 (USER/ASSISTANT 스타일, MCP 뱃지)
- [x] `StreamingCursor.vue` 작성 (깜빡이는 커서)
- [x] `MessageInput.vue` 작성 (auto-resize textarea, 전송 버튼, AI 응답 중 비활성)
- [x] 빈 채팅 상태 예시 프롬프트 카드 3개 구현
- [x] 삭제 확인 모달 구현

### M2 완료 기준
- [ ] 메시지 입력 → AI 스트리밍 응답 화면에 실시간 표시
- [ ] DONE 수신 시 커서 제거, 메시지 완성
- [ ] 대화 히스토리 저장 후 새로고침해도 유지
- [ ] 사이드바 대화 목록 → 클릭하여 이전 대화 불러오기

---

## M3. Gmail Tool Use 연동

### 백엔드
- [x] `GoogleMcpTokenManager` 작성
  - Redis에서 `google_token:{userId}` 조회
  - 만료 시 Google Refresh Token으로 자동 갱신
- [x] `GoogleToolExecutor` 작성 (Gmail 파트)
  - `get_gmail_messages(query, max_results)` → Gmail messages.list + messages.get
  - `send_gmail(to, subject, body)` → Gmail messages.send
- [x] `ClaudeService` 업데이트: Tool Use 루프 구현 (`tools` 파라미터 + `executeStreamRound`)
- [x] `ChatWebSocketHandler` 업데이트: MCP_CALL 타입 처리 (도구 표시명 전달)
- [ ] Gmail Tool Use 테스트 시나리오:
  - "최근 이메일 목록 보여줘"
  - "발신자 {이름} 이메일 찾아줘"
  - "팀장님께 감사 메일 보내줘"
- [x] `DashboardService` 기본 구조 작성 (Gmail 미읽은 수 직접 조회)
- [x] `ErrorCode` MCP 카테고리 추가 (`MCP_001~005`)

### 프론트엔드
- [x] `messageStore` 업데이트: `mcpCallName` 상태로 "Gmail 확인 중..." 표시
- [x] `MessageItem.vue` 업데이트: ASSISTANT 메시지에 MCP 뱃지 표시
- [x] MCP 에러 시 "Google 계정 재연결" 안내 배너 컴포넌트 작성
- [x] Toast 알림 유틸리티 작성 (에러/성공 알림)

### M3 완료 기준
- [ ] "오늘 읽지 않은 이메일 요약해줘" → Gmail 조회 → AI 요약 응답
- [ ] "최근 이메일에 답장 초안 써줘" → 이메일 읽기 → 초안 생성

---

## M4. Google Calendar Tool Use 연동

### 백엔드
- [x] `GoogleToolExecutor` 업데이트 (Calendar 파트)
  - `get_calendar_events(start_date, end_date)` → Calendar events.list
  - 시간 파라미터는 `Instant.toString()` (UTC/Z 형식) 사용 — `+09:00` URL 인코딩 문제 방지
- [ ] Calendar Tool Use 테스트 시나리오:
  - "오늘 일정 알려줘"
  - "이번 주 일정 정리해줘"
  - "다음 주 화요일 오후 3시에 {제목} 일정 잡아줘"
- [x] `DashboardService` 업데이트: 오늘 일정 목록 조회 추가 (UTC 시간 형식 적용)

### 프론트엔드
- [x] 없음 (M3에서 Tool Use 연동 UI 완성)

### M4 완료 기준
- [ ] "오늘 일정 알려줘" → Calendar 조회 → 일정 목록 응답
- [ ] "이번 주 일정 정리해줘" → 실제 일정 데이터 기반 답변

---

## M5. Google Drive Tool Use 연동 + 대시보드

### 백엔드
- [x] `GoogleToolExecutor` 업데이트 (Drive 파트)
  - `get_drive_files(max_results)` → Drive files.list
- [ ] Drive Tool Use 테스트 시나리오:
  - "최근 수정한 파일 보여줘"
  - "최근 드라이브 파일 요약해줘"
- [x] `DashboardService` 업데이트: 최근 Drive 파일 목록 추가
- [x] `DashboardController` 작성 (`GET /api/dashboard`)
- [x] Redis 대시보드 캐시 5분 적용
- [x] `MyBatis DashboardMapper` 작성 (대화 목록 + 마지막 메시지 미리보기)
- [x] `api/dashboard.js` (프론트) 연결용 응답 DTO 완성

### 프론트엔드
- [x] `dashboardStore` 작성 (이메일 수, 일정, 파일)
- [x] `api/dashboard.js` 작성
- [x] `DashboardView.vue` 작성 (3열 위젯 그리드)
- [x] `EmailWidget.vue` 작성 (미읽은 수 뱃지 + 이메일 3건 목록)
- [x] `CalendarWidget.vue` 작성 (오늘 일정 목록)
- [x] `DriveWidget.vue` 작성 (최근 파일 3건)
- [x] 위젯 "채팅에서 열기" 클릭 → 채팅 페이지 + 컨텍스트 메시지 자동 입력
- [x] 대시보드 로딩 스켈레톤 UI 구현
- [x] QuickInputBar 구현 (입력 → `/chat`으로 이동 + 메시지 자동 전송)

### M5 완료 기준
- [ ] 대시보드에서 Gmail/Calendar/Drive 데이터 정상 표시
- [ ] "최근 기획서 찾아서 요약해줘" → Drive 검색 → 요약 응답
- [ ] 위젯 클릭 → 채팅으로 연결 동작

---

## M6. UI 완성 + QA + 배포 준비

### UI 완성
- [x] 로그인 페이지 UI 완성 (로고, Google 버튼 스타일링)
- [x] 전체 다크/라이트 모드 CSS 변수 적용 확인
- [ ] 반응형 레이아웃 최종 점검 (최소 너비 1024px)
- [x] 에러 상태 UI 전체 구현
  - 에러 메시지 버블 + 재시도 버튼
  - 연결 끊김 상단 배너
  - [ ] 대시보드 위젯 에러 상태
- [x] 빈 상태 UI 전체 구현
  - 대화 목록 없음 상태
  - 채팅 빈 상태 + 예시 프롬프트
- [x] 로딩 스피너/스켈레톤 전체 적용
- [x] 페이지 타이틀 (`<title>`) 동적 업데이트

### QA
- [ ] M1 시나리오 재검증: 로그인 → 토큰 갱신 → 로그아웃
- [ ] M2 시나리오 재검증: 채팅 스트리밍 → 히스토리 → 대화 삭제
- [ ] M3 시나리오 재검증: Gmail 이메일 요약 → 답장 초안
- [ ] M4 시나리오 재검증: 일정 조회 → 일정 생성
- [ ] M5 시나리오 재검증: Drive 검색 → 요약 → 대시보드 위젯
- [ ] 복합 시나리오: "오늘 일정이랑 이메일 요약해줘" (Calendar + Gmail 동시 호출)
- [ ] 에러 케이스 테스트
  - Access Token 만료 시 자동 갱신
  - AI 타임아웃 시 에러 버블 표시
  - WebSocket 연결 끊김 시 재연결 배너
- [ ] 브라우저 호환성 확인 (Chrome 최신, Edge)

### 배포 준비
- [x] `backend/Dockerfile` 작성
- [x] `frontend/Dockerfile` 작성
- [x] `docker-compose.prod.yml` 작성 (운영용)
- [x] `README.md` 작성 (프로젝트 소개, 로컬 실행 방법)
- [ ] GitHub 저장소 생성 및 초기 커밋

### M6 완료 기준
- [ ] 모든 M1~M5 시나리오 정상 동작
- [ ] 에러/빈 상태 UI 모두 표시 확인
- [ ] Docker 이미지 빌드 성공
- [ ] README.md 작성 완료
- [ ] GitHub 푸시 완료

---

## 개발 우선순위 원칙

1. **동작 우선** — 스타일보다 기능이 먼저. 각 마일스톤은 동작하는 기능 단위로 완결된다.
2. **수직 슬라이스** — 각 기능은 백엔드-프론트엔드를 같이 구현하여 E2E 동작 가능 상태를 유지한다.
3. **테스트 우선 확인** — 구현 후 반드시 시나리오 기반으로 손으로 테스트한다.
4. **도구 점진적 추가** — Gmail → Calendar → Drive 순서로 하나씩 추가하며 안정성 확인 후 다음 단계로 넘어간다.

---

## 기술 의존성 순서

```
M1 (인증) → M2 (채팅) → M3 (Gmail) → M4 (Calendar) → M5 (Drive + 대시보드) → M6 (완성)
```

- M2는 M1(JWT 인증)이 완료되어야 WebSocket 인증을 구현할 수 있다.
- M3~M5는 M2(ClaudeService + Tool Use 기반 구조)가 완료되어야 도구를 추가할 수 있다.
- M5 대시보드는 M3~M4 Google API 연동이 동작해야 실제 데이터를 표시할 수 있다.
- M3, M4, M5의 도구 추가 작업은 병렬 진행 가능하다 (GoogleToolExecutor 공유).

---

## 참고 문서

| 문서 | 경로 |
|------|------|
| PRD | `1-PRD.md` |
| User Flow | `2-UserFlow.md` |
| Architecture | `3-Architecture.md` |
| DB Schema | `4-DBSchema.md` |
| API Spec | `5-APISpec.md` |
| UI Spec | `6-UISpec.md` |
| Error Spec | `7-ErrorSpec.md` |
| Env Spec | `8-EnvSpec.md` |
| Claude Code 지침 | `CLAUDE.md` |
