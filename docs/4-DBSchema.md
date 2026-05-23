# SmartFlow — DB Schema

> 버전: 1.0.0  
> 작성일: 2026-04-18  
> DBMS: MySQL 8.0  
> Character Set: utf8mb4 / Collation: utf8mb4_unicode_ci

---

## 1. 테이블 목록

| 테이블명 | 설명 | 비고 |
|---------|------|------|
| `users` | 서비스 사용자 | Google OAuth 기반 |
| `conversations` | AI 대화 세션 | 사용자 1명 : 대화 N개 |
| `messages` | 대화 내 메시지 | 대화 1개 : 메시지 N개 |

---

## 2. 테이블 정의

### 2.1 users

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| `id` | BIGINT | NOT NULL | AUTO_INCREMENT | PK |
| `google_id` | VARCHAR(255) | NOT NULL | - | Google OAuth sub (unique) |
| `email` | VARCHAR(255) | NOT NULL | - | 이메일 (unique) |
| `name` | VARCHAR(100) | NOT NULL | - | 사용자 이름 |
| `profile_image_url` | VARCHAR(500) | NULL | NULL | 프로필 이미지 URL |
| `google_access_token` | TEXT | NULL | NULL | Google OAuth Access Token (암호화 저장) |
| `google_refresh_token` | TEXT | NULL | NULL | Google OAuth Refresh Token (암호화 저장) |
| `google_token_expires_at` | DATETIME | NULL | NULL | Google Token 만료 시각 |
| `created_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `uk_users_google_id` (`google_id`)
- UNIQUE INDEX: `uk_users_email` (`email`)

---

### 2.2 conversations

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| `id` | BIGINT | NOT NULL | AUTO_INCREMENT | PK |
| `user_id` | BIGINT | NOT NULL | - | FK → users.id |
| `title` | VARCHAR(200) | NOT NULL | '새 대화' | 대화 제목 (첫 메시지 기반 자동 생성) |
| `created_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 마지막 메시지 시각 (정렬 기준) |

**인덱스**
- PRIMARY KEY: `id`
- INDEX: `idx_conversations_user_id` (`user_id`) — 사용자별 목록 조회
- INDEX: `idx_conversations_user_updated` (`user_id`, `updated_at` DESC) — 최신순 정렬

**외래키**
- `user_id` → `users.id` ON DELETE CASCADE

---

### 2.3 messages

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| `id` | BIGINT | NOT NULL | AUTO_INCREMENT | PK |
| `conversation_id` | BIGINT | NOT NULL | - | FK → conversations.id |
| `role` | ENUM('USER','ASSISTANT') | NOT NULL | - | 발화 주체 |
| `content` | LONGTEXT | NOT NULL | - | 메시지 본문 (Markdown 포함 가능) |
| `mcp_tools_used` | JSON | NULL | NULL | 호출된 MCP 도구 목록 (기록용) |
| `created_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |

**인덱스**
- PRIMARY KEY: `id`
- INDEX: `idx_messages_conversation_id` (`conversation_id`) — 대화별 메시지 조회
- INDEX: `idx_messages_conv_created` (`conversation_id`, `created_at` ASC) — 시간순 정렬

**외래키**
- `conversation_id` → `conversations.id` ON DELETE CASCADE

**mcp_tools_used JSON 예시**
```json
[
  {
    "tool": "gmail",
    "action": "list_emails",
    "called_at": "2026-04-18T10:30:00"
  },
  {
    "tool": "calendar",
    "action": "list_events",
    "called_at": "2026-04-18T10:30:01"
  }
]
```

---

## 3. DDL (CREATE TABLE)

```sql
CREATE DATABASE IF NOT EXISTS smartflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smartflow;

-- users
CREATE TABLE users (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    google_id               VARCHAR(255)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL,
    name                    VARCHAR(100)    NOT NULL,
    profile_image_url       VARCHAR(500)    NULL,
    google_access_token     TEXT            NULL,
    google_refresh_token    TEXT            NULL,
    google_token_expires_at DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_google_id (google_id),
    UNIQUE KEY uk_users_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- conversations
CREATE TABLE conversations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL DEFAULT '새 대화',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conversations_user_id      (user_id),
    KEY idx_conversations_user_updated (user_id, updated_at DESC),
    CONSTRAINT fk_conversations_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- messages
CREATE TABLE messages (
    id              BIGINT                      NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT                      NOT NULL,
    role            ENUM('USER','ASSISTANT')    NOT NULL,
    content         LONGTEXT                    NOT NULL,
    mcp_tools_used  JSON                        NULL,
    created_at      DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_messages_conversation_id (conversation_id),
    KEY idx_messages_conv_created    (conversation_id, created_at ASC),
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 4. Redis Key 구조

SmartFlow에서 Redis는 세션 관리, 토큰 저장, 캐시의 3가지 목적으로 사용된다.

| Key 패턴 | 값 타입 | TTL | 용도 |
|----------|---------|-----|------|
| `smartflow:refresh:{userId}` | String | 7일 | JWT Refresh Token |
| `smartflow:google_token:{userId}` | Hash | 토큰 만료 시각까지 | Google OAuth Token 세트 |
| `smartflow:dashboard:{userId}` | String (JSON) | 5분 | 대시보드 캐시 |
| `smartflow:conv_list:{userId}` | String (JSON) | 10분 | 대화 목록 캐시 |

### 4.1 smartflow:refresh:{userId}
```
VALUE: "eyJhbGci..." (JWT Refresh Token 문자열)
TTL:   604800초 (7일)
SET 시점: 로그인 성공, 토큰 갱신
DEL 시점: 로그아웃, 강제 만료
```

### 4.2 smartflow:google_token:{userId}
```
HSET 필드:
  access_token  → "ya29.xxx..."
  expires_at    → "2026-04-18T11:30:00"
TTL: google_token_expires_at - now
SET 시점: 로그인 성공, Google Token 갱신
```

### 4.3 smartflow:dashboard:{userId}
```
VALUE: JSON 직렬화된 DashboardResponse
TTL:   300초 (5분)
SET 시점: 대시보드 최초 로딩
INVALIDATE: 로그아웃 시 삭제
```

### 4.4 smartflow:conv_list:{userId}
```
VALUE: JSON 직렬화된 List<ConversationSummary>
TTL:   600초 (10분)
SET 시점: 대화 목록 조회
INVALIDATE: 대화 생성/삭제 시 삭제
```

---

## 5. JPA Entity 매핑 요약

```java
// User.java
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "google_id", unique = true, nullable = false)
    private String googleId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // Google Token 필드 (암호화 컨버터 적용)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "google_access_token", columnDefinition = "TEXT")
    private String googleAccessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "google_refresh_token", columnDefinition = "TEXT")
    private String googleRefreshToken;

    @Column(name = "google_token_expires_at")
    private LocalDateTime googleTokenExpiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// Conversation.java
@Entity @Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "conversation", cascade = ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// Message.java
@Entity @Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;  // USER | ASSISTANT

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "mcp_tools_used", columnDefinition = "JSON")
    private String mcpToolsUsed;  // JSON 문자열

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

---

## 6. MyBatis 대시보드 쿼리 (DashboardMapper.xml 예시)

```xml
<!-- 사용자의 최근 대화 목록 + 마지막 메시지 미리보기 -->
<select id="findConversationSummaries" resultType="ConversationSummaryDto">
    SELECT
        c.id,
        c.title,
        c.updated_at,
        m.content   AS last_message,
        m.role      AS last_role
    FROM conversations c
    LEFT JOIN messages m ON m.id = (
        SELECT id FROM messages
        WHERE conversation_id = c.id
        ORDER BY created_at DESC
        LIMIT 1
    )
    WHERE c.user_id = #{userId}
    ORDER BY c.updated_at DESC
    LIMIT #{limit}
</select>
```

---

## 7. 데이터 흐름 요약

```
로그인 성공
  └─ users INSERT/UPDATE (google_id 기준 upsert)
  └─ Redis SET smartflow:refresh:{id}
  └─ Redis HSET smartflow:google_token:{id}

새 대화 시작
  └─ conversations INSERT (title = '새 대화')
  └─ Redis DEL smartflow:conv_list:{userId}  ← 캐시 무효화

메시지 전송
  └─ messages INSERT (role = USER)
  └─ [AI 응답 완료 후] messages INSERT (role = ASSISTANT, mcp_tools_used 포함)
  └─ conversations UPDATE updated_at
  └─ [첫 메시지인 경우] conversations UPDATE title (AI로 자동 생성)

대화 삭제
  └─ conversations DELETE (CASCADE → messages 자동 삭제)
  └─ Redis DEL smartflow:conv_list:{userId}

로그아웃
  └─ Redis DEL smartflow:refresh:{userId}
  └─ Redis DEL smartflow:google_token:{userId}
  └─ Redis DEL smartflow:dashboard:{userId}
```
