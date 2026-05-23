# SmartFlow

**Gmail · Google Calendar · Google Drive를 Claude AI로 자연어 통합 제어하는 개인 업무 자동화 플랫폼**

> Claude Code(AI 코딩 에이전트)를 활용한 **AI-assisted 풀스택 프로젝트**
> 단순한 "AI가 코드를 써줬다"가 아닌, **기획 → 설계 → 구현** 전 과정을 AI와 협업하여 완성한 프로젝트입니다.

---

## 프로젝트 소개

반복적인 이메일 확인, 일정 관리, 문서 검색에서 발생하는 **컨텍스트 전환 비용**을 해결하기 위해 만든 AI 업무 비서입니다.
하나의 채팅 인터페이스에서 자연어로 모든 Google Workspace 도구를 제어할 수 있습니다.

```
사용자: 오늘 일정이랑 안 읽은 이메일 요약해줘
SmartFlow: 오늘 일정을 확인하고 Gmail을 살펴볼게요.

           📅 오늘 일정 (3건)
           - 14:00 팀 스프린트 회의
           - 16:00 디자인 리뷰
           - 18:00 1:1 미팅

           📧 읽지 않은 이메일 (5건)
           - [OOO] 기획안 피드백 요청 — 어제 오후
           - [시스템] 배포 완료 알림 — 오늘 오전
           ...
```

---

## 화면

<!-- 스크린샷 또는 데모 영상을 여기에 추가하세요 -->

---

## AI 활용 개발 방식 (핵심 어필 포인트)

이 프로젝트에서 가장 강조하고 싶은 부분은 **코드 자체가 아니라, AI를 도구로 활용하는 능력**입니다.

### 1. 체계적인 스펙 문서 주도 개발

코드를 한 줄 작성하기 전에 **8개의 설계 문서**를 먼저 완성하고, 이를 AI에게 컨텍스트로 제공해 구현했습니다.

```
docs/
├── 1-PRD.md          # 기능 명세, 사용자 시나리오, 마일스톤
├── 2-UserFlow.md     # 상세 유저 플로우 (분기 조건, 엣지 케이스)
├── 3-Architecture.md # 시스템 구조, Tool Use 흐름, 인증 구조
├── 4-DBSchema.md     # 테이블 설계, 인덱스 전략, DDL
├── 5-APISpec.md      # REST + WebSocket STOMP 전체 API 명세
├── 6-UISpec.md       # 페이지 레이아웃, 컴포넌트 스펙, 디자인 토큰
├── 7-ErrorSpec.md    # 에러 코드 목록, 프론트 처리 방식
└── 8-EnvSpec.md      # 환경 변수 목록, Google OAuth 가이드
```

> "어떻게 만들지"보다 **"무엇을 만들지"를 먼저 정의**하고, AI가 그 스펙을 따르도록 지시한 것이 핵심입니다.

### 2. 단계별 구현 계획 (plan.md)

무작정 구현하지 않고 **6개의 마일스톤**으로 작업을 분할하고, 의존성 순서를 설계했습니다.

```
M1  Google OAuth 인증 + JWT 세션 관리
M2  AI 채팅 기본 기능 (WebSocket 스트리밍)
M3  Gmail Tool Use 연동 (이메일 조회·검색·발송)
M4  Google Calendar Tool Use 연동 (일정 조회·생성)
M5  Google Drive Tool Use 연동 + 대시보드
M6  UI 완성 + QA + 배포 준비
```

> 각 마일스톤이 완료되어야 다음으로 진행하는 규칙을 두어 AI가 충돌 없는 코드를 생성하도록 통제했습니다.

### 3. CLAUDE.md를 통한 AI 행동 규칙 정의

프로젝트 루트에 `CLAUDE.md`를 두어 AI가 코드를 작성할 때 **반드시 따라야 할 규칙**을 명시했습니다.

- Google OAuth Token은 DB에 AES-256-GCM 암호화 저장 강제 (`EncryptedStringConverter`)
- AI 스트리밍은 반드시 `DONE` 또는 `ERROR`로 종료 (미종료 시 입력창 영구 비활성 방지)
- 에러 처리 패턴 통일 (`SmartflowException(ErrorCode.XXX) → GlobalExceptionHandler`)
- Tool Use 순차 추가 — Gmail → Calendar → Drive (검증 후 다음 도구 추가)
- 로그에 API Key, Token, 비밀번호 출력 절대 금지

> AI에게 자유를 주는 것이 아니라 **일관성 있는 아키텍처를 유지하도록 제약**을 설계한 점이 핵심입니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Frontend | Vue.js 3 (Composition API), Vite, Pinia, Vue Router 4, Axios |
| Backend | Spring Boot 3.3, Spring Security, JPA, MyBatis |
| 실시간 | WebSocket (STOMP over SockJS) |
| 인증 | JWT (Access Token + Refresh Token), Google OAuth 2.0 |
| AI | Anthropic Claude API (claude-sonnet-4-6) + Tool Use |
| DB | MySQL 8.0 |
| 캐시 | Redis 7 |
| 인프라 | Docker Compose |

---

## 주요 기능

### AI 채팅 인터페이스
- 자연어 업무 지시 → Claude AI가 의도 파악 → Tool Use로 Google API 자동 실행
- WebSocket(STOMP) 기반 실시간 스트리밍 응답
- 도구 호출 중 "Google Calendar 확인 중..." MCP 뱃지 표시
- 대화 세션 단위 히스토리 보존 및 자동 제목 생성

### Gmail 연동
- 이메일 목록 조회·검색 (발신자, 키워드, 날짜 조건)
- 이메일 본문 읽기 및 AI 요약
- 자연어 지시로 답장 초안 작성
- 이메일 발송 (`send_gmail` 도구)

### Google Calendar 연동
- 오늘 / 이번 주 / 특정 기간 일정 조회
- 자연어로 새 일정 생성 ("내일 오후 2시에 팀 미팅 잡아줘")
- 일정 수정 및 삭제

### Google Drive 연동
- 파일명·키워드로 문서 검색
- 최근 수정 파일 목록 조회
- 문서 내용 읽기 및 AI 요약

### 대시보드
- Gmail 읽지 않은 이메일 수 + 목록 미리보기
- 오늘의 Google Calendar 일정 위젯
- 최근 수정한 Drive 파일 위젯
- 위젯 클릭 → 채팅으로 이동 + 컨텍스트 메시지 자동 입력

### 보안
- JWT Access Token (1시간) + Refresh Token (7일, Redis TTL)
- Access Token 만료 시 Axios 인터셉터가 자동 갱신 후 원래 요청 재시도
- Google OAuth Token은 AES-256-GCM 암호화 후 DB 저장
- WebSocket 연결 시 STOMP 헤더의 JWT 검증

---

## 아키텍처

### Tool Use 처리 흐름

```
사용자 자연어 입력 (WebSocket STOMP)
    │
    ▼
Spring Boot (사용자 메시지 DB 저장)
    │
    ▼
Anthropic Claude API (tools 파라미터 포함)
    │
    ├── stop_reason: "end_turn"  ──▶ 텍스트 응답 스트리밍 → DONE
    │
    └── stop_reason: "tool_use"
              │
              ▼
        GoogleToolExecutor
        ┌──────────┬─────────────┬────────────┐
        ▼          ▼             ▼            ▼
     Gmail     Calendar       Drive      (확장 가능)
    REST API   REST API      REST API
        │          │             │
        └──────────┴─────────────┘
                   │ tool_result
                   ▼
        Anthropic Claude API (재요청)
                   │
                   ▼
        최종 답변 스트리밍 → DONE
```

### WebSocket 스트리밍 청크 타입

```
[Anthropic SSE] ──▶ [Spring Boot] ──▶ [WebSocket /topic/chat/{id}]

{ type: "CHUNK",    content: "텍스트 조각" }       → 실시간 렌더링
{ type: "MCP_CALL", content: "Google Calendar" }   → 도구 호출 뱃지 표시
{ type: "DONE",     messageId: 123 }               → 메시지 완성
{ type: "ERROR",    content: "에러 메시지" }        → 에러 버블 표시
```

### 인증 흐름

```
Google OAuth 로그인
    │ (gmail, calendar, drive scope)
    ▼
Google Access Token + Refresh Token
    │
    ▼
DB 저장 (AES-256 암호화) + Redis 캐시
(key: smartflow:google_token:{userId})
    │
    ▼
Tool Use 실행 시 GoogleMcpTokenManager.getAccessToken()
→ Redis 캐시 조회 → 만료 시 자동 갱신
→ Google REST API 호출 시 Bearer 헤더에 포함
```

---

## 실행 방법

### 사전 요구사항

| 도구 | 버전 |
|------|------|
| Java | 17 이상 |
| Node.js | 18 이상 |
| Docker Desktop | 최신 |
| MySQL | 8.x (Docker로 실행) |
| Redis | 7.x (Docker로 실행) |

### 1. Google Cloud 설정

1. [Google Cloud Console](https://console.cloud.google.com/) → OAuth 2.0 클라이언트 ID 생성 (웹 애플리케이션)
2. 승인된 리디렉션 URI 추가: `http://localhost:5173/oauth/callback`
3. 다음 API 활성화: Gmail API · Google Calendar API · Google Drive API

### 2. 환경 변수 설정

```bash
cp backend/.env.local.sample backend/.env.local
# 파일을 열고 GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, ANTHROPIC_API_KEY 등 입력
```

`frontend/.env` 파일 생성:

```
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws-smartflow
VITE_GOOGLE_REDIRECT_URI=http://localhost:5173/oauth/callback
```

### 3. 실행

```bash
# 인프라 (MySQL + Redis)
docker-compose up -d

# 백엔드
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드
cd frontend
npm install
npm run dev
```

`http://localhost:5173` 접속 후 Google 로그인.

---

## 프로젝트 구조

```
smartflow/
├── backend/                          # Spring Boot 백엔드
│   └── src/main/java/com/smartflow/
│       ├── config/                   # Security, WebSocket, Redis, CORS, Claude API 설정
│       ├── controller/               # Auth, Conversation, Dashboard REST API
│       ├── websocket/                # ChatWebSocketHandler, ChatMessageBroker
│       ├── service/
│       │   ├── ai/                   # ClaudeService (Tool Use 루프), GoogleToolExecutor
│       │   ├── auth/                 # AuthService, JwtService, GoogleOAuthClient
│       │   └── mcp/                  # GoogleMcpTokenManager (Token 캐싱·갱신)
│       ├── domain/                   # User, Conversation, Message (JPA Entity)
│       ├── repository/               # UserRepository, ConversationRepository, MessageRepository
│       ├── mapper/                   # DashboardMapper (MyBatis)
│       ├── dto/                      # Request/Response DTO
│       ├── exception/                # GlobalExceptionHandler, SmartflowException, ErrorCode
│       └── security/                 # JwtAuthenticationFilter, UserPrincipal
│
├── frontend/                         # Vue.js 3 프론트엔드
│   └── src/
│       ├── api/                      # Axios 인스턴스 + 도메인별 API 모듈
│       ├── composables/              # useWebSocket, useStreamingMessage, useAuth
│       ├── stores/                   # Pinia 전역 상태 (auth, conversation, message, dashboard)
│       ├── router/                   # 라우트 + 네비게이션 가드
│       ├── views/                    # LoginView, DashboardView, ChatView
│       └── components/
│           ├── layout/               # AppSidebar, AppHeader
│           ├── chat/                 # MessageList, MessageItem, MessageInput, StreamingCursor
│           └── dashboard/            # EmailWidget, CalendarWidget, DriveWidget
│
├── docs/                             # 8개 설계 문서 (구현 전 작성)
├── CLAUDE.md                         # AI 코딩 에이전트 행동 규칙
├── plan.md                           # 6개 마일스톤 구현 체크리스트
└── docker-compose.yml                # 로컬 개발용 인프라 (MySQL + Redis)
```

---

## 이 프로젝트에서 배운 것

### AI 협업에서 가장 중요한 것: 명확한 제약

AI가 코드를 잘 작성하게 하려면 **개발자가 먼저 아키텍처를 결정해야 합니다.**
막연하게 "Google Calendar 연동해줘"가 아니라:

- "Tool Use는 `tools` 파라미터로 정의하고, `stop_reason: tool_use` 시 GoogleToolExecutor가 직접 Google REST API를 호출한다."
- "Google OAuth Token은 반드시 `EncryptedStringConverter`를 통해 AES-256-GCM으로 암호화 후 저장한다."
- "스트리밍 응답은 반드시 `DONE` 또는 `ERROR` 타입으로 종료되어야 한다. 미종료 시 프론트엔드 입력창이 영구 비활성화된다."

이런 **구체적이고 이유가 있는 제약** 덕분에 일관성 있는 코드가 나왔습니다.

### 설계가 먼저, 코드는 그 다음

8개의 설계 문서를 직접 작성하며 비로소:
- Claude의 **Tool Use**가 단순 프롬프트와 어떻게 다른지 (`stop_reason: "tool_use"` → 백엔드 실행 → `tool_result` 재전송)
- Google OAuth Token을 서버에서 관리할 때 Redis 캐싱 + 자동 갱신이 왜 필요한지
- Anthropic SSE 스트리밍을 백엔드에서 WebSocket으로 변환하는 구조가 왜 더 나은지

를 깊이 이해하게 됐습니다. AI는 그 이해를 코드로 옮기는 도구였습니다.

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [PRD](docs/1-PRD.md) | 기능 명세, 사용자 시나리오, 마일스톤 |
| [Architecture](docs/3-Architecture.md) | 시스템 구조, Tool Use·인증·스트리밍 흐름도 |
| [API Spec](docs/5-APISpec.md) | REST + WebSocket STOMP 전체 API |
| [DB Schema](docs/4-DBSchema.md) | 테이블 설계, DDL, Redis 키 구조 |
| [Error Spec](docs/7-ErrorSpec.md) | 에러 코드 목록, 프론트 처리 방식 |
| [Plan](plan.md) | 6개 마일스톤 구현 체크리스트 |

---

## ERD

<!-- ERD 이미지를 여기에 추가하세요 -->
