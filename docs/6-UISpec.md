# SmartFlow — UI Spec

> 버전: 1.0.0
> 작성일: 2026-04-18
> 프레임워크: Vue.js 3 + Vite

---

## 1. 디자인 토큰

### 1.1 컬러
CSS 변수 기반, 다크 모드 자동 대응.

| 용도 | 변수 | 비고 |
|------|------|------|
| 페이지 배경 | `--color-background-tertiary` | 사이드바 + 본문 베이스 |
| 카드/패널 배경 | `--color-background-primary` | 채팅 영역, 위젯 카드 |
| 서피스 (hover) | `--color-background-secondary` | 리스트 아이템 hover |
| 본문 텍스트 | `--color-text-primary` | |
| 보조 텍스트 | `--color-text-secondary` | 타임스탬프, 힌트 |
| 브랜드 액센트 | `#1D9E75` (Teal 400) | 전송 버튼, AI 아바타 |
| 경계선 | `--color-border-tertiary` | 0.5px solid |

### 1.2 타이포그래피
| 스타일 | 크기 | 굵기 | 용도 |
|--------|------|------|------|
| heading-1 | 22px | 500 | 페이지 제목 |
| heading-2 | 18px | 500 | 섹션 제목 |
| heading-3 | 16px | 500 | 카드 제목 |
| body | 15px | 400 | 메시지, 본문 |
| small | 13px | 400 | 타임스탬프, 힌트 |
| mono | 14px | 400 | 코드 블록 |

### 1.3 스페이싱
| 토큰 | 값 | 용도 |
|------|-----|------|
| `--space-xs` | 4px | 아이콘 내부 여백 |
| `--space-sm` | 8px | 컴포넌트 내부 |
| `--space-md` | 16px | 카드 패딩 |
| `--space-lg` | 24px | 섹션 간격 |
| `--space-xl` | 40px | 페이지 레이아웃 |

### 1.4 Border Radius
| 토큰 | 값 | 용도 |
|------|-----|------|
| `--border-radius-sm` | 6px | 뱃지, 작은 버튼 |
| `--border-radius-md` | 8px | 입력창, 버튼 |
| `--border-radius-lg` | 12px | 카드, 메시지 버블 |
| `--border-radius-xl` | 20px | 메시지 버블 (꼬리 반대편) |

---

## 2. 레이아웃 구조

### 2.1 전체 레이아웃
```
┌─────────────────────────────────────────────┐
│          AppHeader (인증 없는 페이지만)         │
├─────────────┬───────────────────────────────┤
│             │                               │
│  AppSidebar │       <router-view>           │
│   260px     │   Dashboard 또는 ChatView      │
│   (고정)    │                               │
│             │                               │
└─────────────┴───────────────────────────────┘
```

- 사이드바는 `/login`에서 숨김
- 메인 영역 최소 너비: 600px
- 브레이크포인트: 미구현 (데스크탑 전용 MVP)

### 2.2 사이드바 (AppSidebar.vue)
```
┌─────────────┐
│  SmartFlow  │  ← 로고 + 앱명 (20px, 500)
├─────────────┤
│ + 새 대화   │  ← 버튼 (border: 0.5px, 전체 너비)
├─────────────┤
│ 대화 목록   │  ← 섹션 레이블
│ ┌─────────┐ │
│ │ 오늘 일정│ │  ← 대화 아이템 (hover: bg-secondary)
│ │ 10:30   │ │     제목 + 타임스탬프
│ └─────────┘ │
│ ┌─────────┐ │
│ │ Gmail 정│ │  ← 선택됨 (bg-secondary + 좌측 border)
│ │ 어제    │ │
│ └─────────┘ │
├─────────────┤
│ [아바타] 김승현 │  ← 하단 사용자 프로필 + 로그아웃
└─────────────┘
```

---

## 3. 페이지 스펙

### 3.1 로그인 페이지 (LoginView.vue)
```
경로: /login
레이아웃: 사이드바 없음, 화면 중앙 정렬
```

**구성 요소**
| 요소 | 스펙 |
|------|------|
| 로고 | 텍스트 로고 "SmartFlow" (28px, 500) |
| 부제 | "AI로 연결하는 Gmail · Calendar · Drive" (15px, secondary) |
| Google 로그인 버튼 | 너비 320px, 높이 48px, border-radius-md, Google 로고 포함 |
| 버튼 텍스트 | "Google로 계속하기" |

**상태**
- 기본: 버튼 활성
- 로딩: 버튼 비활성 + 스피너
- 오류: 버튼 아래 에러 메시지 (color-text-danger, 13px)

---

### 3.2 대시보드 (DashboardView.vue)
```
경로: /dashboard
레이아웃: 사이드바 + 메인 영역
```

**구성 요소**

| 영역 | 컴포넌트 | 설명 |
|------|---------|------|
| 상단 | 페이지 제목 | "안녕하세요, {name}" (22px) + 날짜 |
| 빠른 입력 | QuickInputBar | "무엇을 도와드릴까요?" placeholder, 엔터로 채팅 이동 |
| 위젯 그리드 | 3열 grid | gap: 16px, grid-template-columns: repeat(3, 1fr) |
| 위젯 1 | EmailWidget | 미읽은 이메일 수 + 최근 3건 목록 |
| 위젯 2 | CalendarWidget | 오늘 일정 목록 (시간순) |
| 위젯 3 | DriveWidget | 최근 수정 파일 3건 |

**EmailWidget**
```
┌──────────────────────┐
│ 받은 편지함          │
│ ●5 읽지 않은 메일    │ ← 카운트 뱃지 (amber)
├──────────────────────┤
│ Google Workspace     │
│ 결제 확인 안내  14:20│
│──────────────────────│
│ Team Notification    │
│ PR 리뷰 요청   13:15 │
│──────────────────────│
│ 채팅에서 열기 →      │ ← 클릭 시 채팅 + 이메일 컨텍스트
└──────────────────────┘
```

**CalendarWidget**
```
┌──────────────────────┐
│ 오늘 일정            │
│ 2026년 4월 18일      │
├──────────────────────┤
│ 14:00  팀 미팅       │ ← 시간 (teal) + 제목
│ 16:00  코드 리뷰     │
│ 없음   저녁 이후     │
│──────────────────────│
│ 일정 추가하기 →      │
└──────────────────────┘
```

**DriveWidget**
```
┌──────────────────────┐
│ 최근 Drive 파일      │
├──────────────────────┤
│ 📄 2026 Q2 기획서    │ ← 파일 아이콘 + 이름
│    어제 수정         │
│ 📊 월별 성과 대시보드│
│    3일 전 수정       │
│──────────────────────│
│ 파일 찾기 →          │
└──────────────────────┘
```

---

### 3.3 채팅 페이지 (ChatView.vue)
```
경로: /chat, /chat/:conversationId
레이아웃: 사이드바 + 채팅 영역 (flex-column, 전체 높이)
```

**채팅 영역 구조**
```
┌───────────────────────────────────┐
│ 오늘 일정 브리핑             [···]│ ← 대화 제목 + 메뉴 (헤더)
├───────────────────────────────────┤
│                                   │
│         [MessageList]             │ ← flex: 1, overflow-y: auto
│                                   │   스크롤 영역
│  ┌─────────────────────────┐      │
│  │ 오늘 일정이랑 이메일    │      │ ← USER 메시지 (오른쪽)
│  │ 요약해줘       10:29    │      │
│  └─────────────────────────┘      │
│                                   │
│  [SF]  Gmail 확인 중...    ←      │ ← MCP_CALL 표시 (로딩)
│                                   │
│  [SF]  오늘 일정은 오후 2시│      │ ← ASSISTANT 메시지 (왼쪽)
│        팀 미팅이 있습니다▌ │      │   ▌= 스트리밍 커서
│                                   │
├───────────────────────────────────┤
│ [MessageInput]                    │ ← 고정 하단
│ ┌─────────────────────────┐ [전송]│
│ │ 메시지를 입력하세요...  │      │
│ └─────────────────────────┘      │
└───────────────────────────────────┘
```

---

## 4. 컴포넌트 스펙

### 4.1 MessageItem.vue

**USER 메시지**
```
오른쪽 정렬
배경: --color-background-secondary
border-radius: 20px 20px 4px 20px
최대 너비: 70%
패딩: 10px 14px
텍스트: 15px, primary
타임스탬프: 오른쪽 아래, 11px, secondary
```

**ASSISTANT 메시지**
```
왼쪽 정렬
아바타: 32px 원형, 배경 Teal 400, 텍스트 "SF" 흰색
배경: --color-background-primary
border: 0.5px solid --color-border-tertiary
border-radius: 4px 20px 20px 20px
최대 너비: 85%
패딩: 12px 16px
텍스트: 15px, Markdown 렌더링 (marked.js)
MCP 뱃지: 메시지 상단, "Gmail 조회됨" (12px, teal 배경)
```

**MCP_CALL 로딩 상태**
```
왼쪽 정렬, 아바타 동일
배경: --color-background-secondary
점 3개 애니메이션 (typing indicator) + "Gmail 확인 중..."
```

### 4.2 MessageInput.vue

```
하단 고정 영역 (border-top: 0.5px)
패딩: 12px 16px

textarea:
  - 최소 높이: 44px
  - 최대 높이: 160px (초과 시 스크롤)
  - auto-resize: 입력에 따라 높이 자동 확장
  - placeholder: "메시지를 입력하세요... (Shift+Enter: 줄바꿈)"
  - border-radius: 12px
  - 비활성 상태: AI 응답 중 (disabled)

전송 버튼:
  - 크기: 36x36px, border-radius: 50%
  - 배경: Teal 400 (#1D9E75)
  - 아이콘: 화살표 위 (SVG)
  - 비활성: 내용 없거나 AI 응답 중
```

### 4.3 AppSidebar.vue

```
너비: 260px (고정)
배경: --color-background-tertiary
border-right: 0.5px solid --color-border-tertiary

새 대화 버튼:
  - 전체 너비, 높이 36px
  - border: 0.5px solid --color-border-secondary
  - hover: background-secondary
  - 텍스트: "+ 새 대화"

대화 아이템:
  - 높이: 60px (제목 + 타임스탬프)
  - hover: background-secondary, border-radius: 8px
  - 활성: background-secondary + border-left: 2px solid Teal 400
  - 삭제 버튼: hover 시만 표시 (우측 끝)
  - 제목 최대: 1줄, overflow ellipsis
```

### 4.4 StreamingCursor.vue
```html
<!-- 스트리밍 중 깜빡이는 커서 -->
<span class="streaming-cursor"></span>

<style>
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--color-text-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0; }
}
</style>
```

---

## 5. 라우터 가드 (router/index.js)

```javascript
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    next('/dashboard')
  } else {
    next()
  }
})

const routes = [
  { path: '/login',     component: LoginView,     meta: { requiresAuth: false } },
  { path: '/dashboard', component: DashboardView, meta: { requiresAuth: true  } },
  { path: '/chat',      component: ChatView,       meta: { requiresAuth: true  } },
  { path: '/chat/:id',  component: ChatView,       meta: { requiresAuth: true  } },
]
```

---

## 6. Pinia 스토어 구조

### auth.js
```javascript
state: {
  user: null,          // { id, email, name, profileImageUrl }
  accessToken: null,   // localStorage 연동
}
getters: {
  isAuthenticated: (state) => !!state.accessToken
}
actions: {
  login(googleCode, redirectUri),
  logout(),
  refreshToken(),
  loadFromStorage()    // 앱 초기화 시 호출
}
```

### conversation.js
```javascript
state: {
  conversations: [],   // 사이드바 목록
  currentId: null,     // 현재 열린 대화 ID
}
actions: {
  fetchList(),
  create(),
  delete(id),
  updateTitle(id, title)
}
```

### message.js
```javascript
state: {
  messages: [],        // 현재 대화 메시지 목록
  isStreaming: false,  // AI 응답 중 여부
  streamingContent: '',// 누적 중인 스트리밍 텍스트
  mcpCallName: null,  // 현재 호출 중인 MCP 이름
}
actions: {
  fetchHistory(conversationId),
  appendChunk(chunk),  // CHUNK 타입 수신 시
  setMcpCall(name),   // MCP_CALL 수신 시
  finishStreaming(messageId), // DONE 수신 시
  handleError(message)        // ERROR 수신 시
}
```

---

## 7. 에러 / 빈 상태 UI

| 상태 | 표시 방법 |
|------|----------|
| 대화 목록 없음 | 사이드바에 "아직 대화가 없습니다" + 새 대화 버튼 |
| 메시지 없음 | 채팅 영역 중앙에 예시 프롬프트 3개 카드 |
| AI 오류 | 빨간 에러 메시지 버블 + "다시 시도" 버튼 |
| 네트워크 오류 | 상단 배너 "연결이 끊겼습니다. 재연결 중..." |
| 대시보드 로딩 실패 | 위젯 카드에 "데이터를 불러올 수 없습니다" |

---

## 8. 예시 프롬프트 (빈 채팅 상태)

채팅이 처음 시작될 때 중앙에 표시하는 예시 카드 3개:

| 카드 | 텍스트 |
|------|--------|
| Gmail | "오늘 받은 중요한 이메일 요약해줘" |
| Calendar | "이번 주 일정 정리해줘" |
| Drive | "최근에 수정한 문서 보여줘" |

클릭 시 해당 텍스트가 입력창에 자동 입력 후 전송.
