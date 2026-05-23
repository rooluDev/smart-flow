# SmartFlow — User Flow

> 버전: 1.0.0
> 작성일: 2026-04-18

---

## 플로우 목록

| ID | 플로우명 | 관련 기능 |
|----|---------|---------|
| UF-01 | 최초 진입 및 인증 | F-06 |
| UF-02 | 대시보드 진입 | F-07 |
| UF-03 | 새 대화 시작 및 AI 채팅 | F-01 |
| UF-04 | Tool Use 도구 자동 선택 흐름 | F-01, F-02, F-03, F-04 |
| UF-05 | 이전 대화 불러오기 | F-05 |
| UF-06 | 대화 삭제 | F-05 |
| UF-07 | 로그아웃 | F-06 |

---

## UF-01. 최초 진입 및 인증

```
[브라우저에서 앱 접속]
        │
        ▼
[로컬 스토리지에 Access Token 존재?]
        │
   YES ─┤─ NO
        │        │
        │        ▼
        │   [로그인 페이지 표시]
        │        │
        │        ▼
        │   [Google로 로그인 버튼 클릭]
        │        │
        │        ▼
        │   [Google OAuth2 동의 화면]
        │   (Gmail, Calendar, Drive 권한 포함)
        │        │
        │   동의 완료
        │        │
        │        ▼
        │   [백엔드: Authorization Code 수신]
        │        │
        │        ▼
        │   [백엔드: Access Token + Refresh Token 발급]
        │   - Access Token → 응답 Body
        │   - Refresh Token → Redis 저장
        │        │
        │        ▼
        │   [프론트엔드: Access Token 로컬 스토리지 저장]
        │        │
        └────────┤
                 ▼
        [Token 유효성 검증 API 호출]
                 │
        유효  ───┤─── 만료
                 │         │
                 │         ▼
                 │    [Refresh Token으로 재발급]
                 │         │
                 │    성공 ─┤─ 실패
                 │         │       │
                 │         │       ▼
                 │         │  [로그인 페이지로 이동]
                 │         │
                 └────────┘
                 │
                 ▼
        [대시보드 페이지로 이동]
```

### 상태 정의
| 상태 | 설명 |
|------|------|
| 비인증 | Access Token 없음 → 로그인 페이지 리다이렉트 |
| 인증됨 | 유효한 Access Token 존재 → 앱 이용 가능 |
| 토큰 만료 | Access Token 만료 → Refresh Token으로 자동 갱신 시도 |
| 갱신 실패 | Refresh Token도 만료 → 재로그인 필요 |

---

## UF-02. 대시보드 진입

```
[대시보드 페이지 마운트]
        │
        ▼
[3개 위젯 병렬 데이터 로딩]
  ┌─────┼─────┐
  ▼     ▼     ▼
[미읽은  [오늘    [최근 Drive
 이메일]  일정]    파일]
  │      │       │
  └──────┴───────┘
        │
  모두 완료
        │
        ▼
[대시보드 렌더링 완료]
        │
        ├── [채팅 빠른입력창] ── 입력 → UF-03으로 이동
        │
        ├── [이메일 항목 클릭] ── "이 이메일 요약해줘" 메시지와 함께 채팅으로 이동
        │
        ├── [일정 항목 클릭] ── "이 일정 상세 알려줘" 메시지와 함께 채팅으로 이동
        │
        └── [Drive 파일 클릭] ── "이 문서 요약해줘" 메시지와 함께 채팅으로 이동
```

---

## UF-03. 새 대화 시작 및 AI 채팅

```
[채팅 페이지 진입]
        │
        ▼
[새 대화 자동 생성 또는 기존 대화 선택]
        │
        ▼
[사용자: 메시지 입력 후 전송]
        │
        ▼
[WebSocket으로 메시지 전송]
        │
        ▼
[백엔드: 메시지 DB 저장]
        │
        ▼
[백엔드: Claude API 호출 시작]
(대화 히스토리 + tools 파라미터 포함)
        │
        ▼
[스트리밍 응답 시작]
        │
        ▼
[프론트엔드: 토큰 단위로 실시간 렌더링]
   ┌────┴────┐
   │         │
MCP 호출    일반 응답
필요 없음    완료
   │
   ▼
[MCP Tool Use 실행] ── UF-04 참조
   │
   ▼
[MCP 결과 포함 최종 응답 스트리밍]
        │
        ▼
[응답 완료]
        │
        ▼
[백엔드: AI 응답 DB 저장]
        │
        ▼
[대화 제목 자동 생성]
(첫 번째 메시지 기준, 첫 대화인 경우)
        │
        ▼
[사용자: 후속 메시지 입력 가능]
```

---

## UF-04. Tool Use 도구 자동 선택 흐름

Claude AI가 사용자 메시지를 분석하여 어떤 도구를 호출할지 자동으로 결정한다.
백엔드(GoogleToolExecutor)가 Google API를 직접 호출하고 결과를 Claude에게 반환한다.

```
[Claude AI: 사용자 메시지 분석]
(tools 파라미터로 사용 가능한 도구 목록 전달됨)
        │
        ▼
[의도 분류]
        │
   ┌────┼────┬────────┐
   ▼    ▼    ▼        ▼
[이메일] [일정] [Drive] [도구 불필요]
관련    관련   관련    (일반 대화)
   │    │     │         │
   ▼    ▼     ▼         ▼
[get_  [get_   [get_    [텍스트
gmail_ calendar drive_   응답 생성]
messages] _events] files]
   │    │     │
   └────┴─────┘
        │
        ▼
[stop_reason: "tool_use" 수신]
        │
        ▼
[백엔드: GoogleToolExecutor → Google REST API 직접 호출]
        │
        ▼
[tool_result 포함하여 Claude 재요청]
        │
        ▼
[복합 요청인 경우: 여러 도구 순차 호출]
예: "오늘 일정이랑 이메일 요약해줘"
→ get_calendar_events → get_gmail_messages 순차 호출
        │
        ▼
[전체 결과를 종합하여 최종 응답 스트리밍]
```

### Gmail 도구 세부 플로우
```
[get_gmail_messages 호출]
(query: "is:unread", "from:xxx", "subject:yyy" 등)
        │
        ▼
[Gmail REST API 직접 호출]
messages.list → messages.get(metadata)
        │
        ▼
[결과: subject, from, date, snippet 목록 반환]
        │
        ▼
[send_gmail 호출 (이메일 발송 요청 시)]
        │
        ▼
[Gmail messages.send API 호출]
        │
        ▼
[발송 완료 확인 반환]
```

### Calendar 도구 세부 플로우
```
[get_calendar_events 호출]
(start_date, end_date: YYYY-MM-DD 형식)
        │
        ▼
[Google Calendar REST API 직접 호출]
calendars/primary/events (UTC 타임존으로 요청)
        │
        ▼
[결과: title, start, end, location 목록 반환]
```

### Drive 도구 세부 플로우
```
[get_drive_files 호출]
(max_results: 조회 개수)
        │
        ▼
[Google Drive REST API 직접 호출]
files (modifiedTime desc 정렬)
        │
        ▼
[결과: name, mimeType, modifiedTime, webViewLink 목록 반환]
```

---

## UF-05. 이전 대화 불러오기

```
[사이드바: 대화 목록 표시]
        │
        ▼
[사용자: 대화 항목 클릭]
        │
        ▼
[해당 대화 ID로 API 호출]
        │
        ▼
[메시지 히스토리 로딩]
        │
        ▼
[채팅창에 이전 대화 렌더링]
        │
        ▼
[사용자: 대화 이어서 입력 가능]
(이전 컨텍스트 유지)
```

---

## UF-06. 대화 삭제

```
[사이드바: 대화 항목에 마우스 오버]
        │
        ▼
[삭제 버튼 표시]
        │
        ▼
[사용자: 삭제 버튼 클릭]
        │
        ▼
[삭제 확인 모달 표시]
"이 대화를 삭제하시겠습니까?"
        │
   확인 ─┤─ 취소
        │       │
        ▼       ▼
[DELETE API    [모달 닫기]
 호출]
        │
        ▼
[사이드바에서 대화 제거]
        │
        ▼
[현재 열려있던 대화였으면
 새 대화 화면으로 이동]
```

---

## UF-07. 로그아웃

```
[헤더: 사용자 프로필 클릭]
        │
        ▼
[드롭다운 메뉴 표시]
        │
        ▼
[로그아웃 클릭]
        │
        ▼
[백엔드: Refresh Token 삭제 (Redis)]
        │
        ▼
[프론트엔드: Access Token 로컬 스토리지 삭제]
        │
        ▼
[로그인 페이지로 이동]
```

---

## 페이지 라우팅 구조

```
/ (루트)
├── /login               ← 비인증 상태 진입점
│
├── /dashboard           ← 인증 후 기본 랜딩 페이지
│
└── /chat                ← 채팅 메인 페이지
    ├── /chat            ← 새 대화 (conversation ID 없음)
    └── /chat/:id        ← 특정 대화 불러오기
```

### 라우팅 가드
- `/login` 이외 모든 경로: 인증 필수 (미인증 시 `/login` 리다이렉트)
- `/login`: 인증 상태에서 접근 시 `/dashboard` 리다이렉트

---

## 주요 엣지 케이스

| 케이스 | 처리 방법 |
|--------|----------|
| Google API 타임아웃 (10초 초과) | Claude가 tool_result에 error 포함 → "요청 시간이 초과되었습니다. 다시 시도해주세요." 응답 |
| Claude API 오류 | "AI 응답 중 오류가 발생했습니다." + 재시도 버튼 |
| WebSocket 연결 끊김 | 자동 재연결 시도 (3회), 실패 시 페이지 새로고침 안내 |
| Google OAuth 권한 거부 | "일부 기능이 제한됩니다." 안내 후 제한된 모드로 진입 |
| 빈 메시지 전송 시도 | 전송 버튼 비활성화 (입력값 없을 때) |
| 대화 히스토리 없는 상태 | "새 대화를 시작해보세요" 안내 메시지 표시 |
