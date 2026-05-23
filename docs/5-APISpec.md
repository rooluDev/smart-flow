# SmartFlow — API Spec

> 버전: 1.0.0  
> 작성일: 2026-04-18  
> Base URL: `http://localhost:8080`  
> 인증: `Authorization: Bearer {accessToken}` (로그인 API 제외)  
> Content-Type: `application/json`

---

## 공통 응답 구조

### 성공 응답
```json
{
  "success": true,
  "data": { ... }
}
```

### 실패 응답
```json
{
  "success": false,
  "error": {
    "code": "AUTH_001",
    "message": "인증이 필요합니다."
  }
}
```

---

## 1. 인증 API

### POST /api/auth/google
Google OAuth 인가 코드를 받아 JWT를 발급한다.

**Request Body**
```json
{
  "code": "4/0AX4XfWh...",
  "redirectUri": "http://localhost:5173/oauth/callback"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `code` | String | Y | Google OAuth 인가 코드 |
| `redirectUri` | String | Y | OAuth 등록된 리다이렉트 URI |

**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@gmail.com",
      "name": "김승현",
      "profileImageUrl": "https://lh3.googleusercontent.com/..."
    }
  }
}
```

**Error Cases**
| 상황 | HTTP | errorCode |
|------|------|-----------|
| 인가 코드 무효 | 400 | `AUTH_002` |
| Google API 오류 | 502 | `AUTH_003` |

---

### POST /api/auth/refresh
만료된 Access Token을 Refresh Token으로 갱신한다.

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**Error Cases**
| 상황 | HTTP | errorCode |
|------|------|-----------|
| Refresh Token 만료 | 401 | `AUTH_006` |
| Refresh Token 불일치 | 401 | `AUTH_007` |

---

### POST /api/auth/logout
Refresh Token을 무효화하고 로그아웃 처리한다.

**Request Header**: `Authorization: Bearer {accessToken}`

**Request Body**: 없음

**Response 200**
```json
{
  "success": true,
  "data": null
}
```

---

## 2. 사용자 API

### GET /api/users/me
현재 로그인한 사용자 정보를 반환한다.

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@gmail.com",
    "name": "김승현",
    "profileImageUrl": "https://lh3.googleusercontent.com/..."
  }
}
```

---

## 3. 대화 API

### GET /api/conversations
사용자의 대화 목록을 최신순으로 반환한다.

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `page` | int | N | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | N | 20 | 페이지 크기 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "id": 42,
        "title": "오늘 일정 브리핑",
        "lastMessage": "오늘 오후 2시에 팀 미팅이 있습니다...",
        "lastMessageRole": "ASSISTANT",
        "updatedAt": "2026-04-18T10:30:00"
      }
    ],
    "totalCount": 15,
    "hasNext": false
  }
}
```

---

### POST /api/conversations
새 대화를 생성한다.

**Request Body**: 없음 (제목은 첫 메시지 전송 후 자동 생성)

**Response 201**
```json
{
  "success": true,
  "data": {
    "id": 43,
    "title": "새 대화",
    "createdAt": "2026-04-18T11:00:00"
  }
}
```

---

### GET /api/conversations/{conversationId}
특정 대화의 메시지 히스토리를 반환한다.

**Path Variables**
| 변수 | 타입 | 설명 |
|------|------|------|
| `conversationId` | Long | 대화 ID |

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "오늘 일정 브리핑",
    "messages": [
      {
        "id": 101,
        "role": "USER",
        "content": "오늘 일정이랑 안 읽은 이메일 요약해줘",
        "mcpToolsUsed": null,
        "createdAt": "2026-04-18T10:29:00"
      },
      {
        "id": 102,
        "role": "ASSISTANT",
        "content": "오늘 일정을 확인했습니다...",
        "mcpToolsUsed": [
          { "tool": "calendar", "action": "list_events" },
          { "tool": "gmail",    "action": "list_emails" }
        ],
        "createdAt": "2026-04-18T10:30:00"
      }
    ],
    "createdAt": "2026-04-18T10:29:00",
    "updatedAt": "2026-04-18T10:30:00"
  }
}
```

**Error Cases**
| 상황 | HTTP | errorCode |
|------|------|-----------|
| 존재하지 않는 대화 | 404 | `CONV_001` |
| 다른 사용자의 대화 | 403 | `CONV_002` |

---

### PATCH /api/conversations/{conversationId}/title
대화 제목을 수정한다. (자동 생성 또는 사용자 직접 수정)

**Request Body**
```json
{
  "title": "Gmail 정리 작업"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `title` | String | Y | 1~200자 | 새 제목 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "Gmail 정리 작업"
  }
}
```

---

### DELETE /api/conversations/{conversationId}
대화를 삭제한다. 연관 메시지도 CASCADE 삭제된다.

**Response 204**: body 없음

**Error Cases**
| 상황 | HTTP | errorCode |
|------|------|-----------|
| 존재하지 않는 대화 | 404 | `CONV_001` |
| 다른 사용자의 대화 | 403 | `CONV_002` |

---

## 4. 대시보드 API

### GET /api/dashboard
대시보드에 표시할 데이터를 한 번에 반환한다. Redis 캐시(5분) 적용.

**Response 200**
```json
{
  "success": true,
  "data": {
    "email": {
      "unreadCount": 5
    },
    "calendar": {
      "todayEvents": [
        {
          "id": "abc123",
          "title": "팀 미팅",
          "startTime": "2026-04-18T14:00:00",
          "endTime":   "2026-04-18T15:00:00",
          "location": "회의실 A"
        }
      ]
    },
    "drive": {
      "recentFiles": [
        {
          "id": "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms",
          "name": "2026 Q2 기획서.docx",
          "mimeType": "application/vnd.google-apps.document",
          "modifiedTime": "2026-04-17T18:00:00",
          "webViewLink": "https://docs.google.com/..."
        }
      ]
    }
  }
}
```

---

## 5. WebSocket API (STOMP)

### 연결 엔드포인트
```
ws://localhost:8080/ws-smartflow
(SockJS fallback 포함)
```

**연결 시 헤더**
```
Authorization: Bearer {accessToken}
```

---

### SEND: /app/chat.send
메시지를 전송하고 AI 스트리밍 응답을 시작한다.

**Payload**
```json
{
  "conversationId": 42,
  "content": "오늘 일정이랑 안 읽은 이메일 요약해줘"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `conversationId` | Long | Y | - | 대화 ID |
| `content` | String | Y | 1~5000자 | 메시지 내용 |

---

### SUBSCRIBE: /topic/chat/{conversationId}
AI 스트리밍 응답을 수신한다.

**수신 메시지 구조**
```json
{
  "type": "CHUNK",
  "content": "오늘 일정을",
  "messageId": null
}
```

| `type` | `content` | `messageId` | 의미 |
|--------|-----------|-------------|------|
| `CHUNK` | 텍스트 조각 | null | 응답 토큰 스트림 |
| `MCP_CALL` | MCP 이름 (예: `"gmail"`) | null | MCP 도구 호출 중 (UI 로딩 표시용) |
| `DONE` | null | 저장된 메시지 ID | 응답 완료, DB 저장 완료 |
| `ERROR` | 에러 메시지 | null | 오류 발생 |

**전체 스트리밍 시퀀스 예시**
```
→ { "type": "MCP_CALL", "content": "calendar" }
→ { "type": "MCP_CALL", "content": "gmail" }
→ { "type": "CHUNK", "content": "오늘" }
→ { "type": "CHUNK", "content": " 일정은" }
→ { "type": "CHUNK", "content": " 오후 2시" }
→ { "type": "CHUNK", "content": " 팀 미팅입니다." }
...
→ { "type": "DONE", "content": null, "messageId": 102 }
```

---

## 6. 엔드포인트 목록 요약

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | /api/auth/google | ✗ | Google OAuth 로그인 |
| POST | /api/auth/refresh | ✗ | Access Token 갱신 |
| POST | /api/auth/logout | ✓ | 로그아웃 |
| GET | /api/users/me | ✓ | 내 정보 조회 |
| GET | /api/conversations | ✓ | 대화 목록 |
| POST | /api/conversations | ✓ | 새 대화 생성 |
| GET | /api/conversations/{id} | ✓ | 대화 + 메시지 조회 |
| PATCH | /api/conversations/{id}/title | ✓ | 대화 제목 수정 |
| DELETE | /api/conversations/{id} | ✓ | 대화 삭제 |
| GET | /api/dashboard | ✓ | 대시보드 데이터 |
| WS SEND | /app/chat.send | ✓ | 채팅 메시지 전송 |
| WS SUB | /topic/chat/{id} | ✓ | 스트리밍 응답 구독 |

---

## 7. HTTP 상태 코드 사용 원칙

| 코드 | 사용 상황 |
|------|---------|
| 200 | 정상 조회, 수정 |
| 201 | 정상 생성 |
| 204 | 정상 삭제 (body 없음) |
| 400 | 요청 값 오류 (유효성 검증 실패) |
| 401 | 인증 실패 (Token 없음, 만료, 불일치) |
| 403 | 권한 없음 (다른 사용자 리소스 접근) |
| 404 | 리소스 없음 |
| 502 | 외부 API 오류 (Google, Anthropic) |
| 500 | 서버 내부 오류 |
