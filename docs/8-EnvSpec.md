# SmartFlow — Env Spec

> 버전: 1.0.0
> 작성일: 2026-04-18

---

## 1. 환경 파일 구조

```
smartflow/
├── backend/
│   ├── src/main/resources/
│   │   ├── application.yml          # 공통 설정 (Git 추적)
│   │   ├── application-local.yml    # 로컬 전용 (Git 무시)
│   │   └── application-prod.yml     # 운영 전용 (Git 무시)
│   └── .env.local                   # 로컬 시크릿 (Git 무시)
│
├── frontend/
│   ├── .env                         # 공통 기본값 (Git 추적)
│   ├── .env.local                   # 로컬 오버라이드 (Git 무시)
│   └── .env.production              # 운영 값 (Git 무시)
│
├── docker-compose.yml               # 로컬 인프라 (Git 추적)
├── docker-compose.override.yml      # 로컬 오버라이드 (Git 무시)
└── .gitignore
```

### .gitignore 필수 항목
```
# 백엔드
backend/src/main/resources/application-local.yml
backend/src/main/resources/application-prod.yml
backend/.env.local

# 프론트엔드
frontend/.env.local
frontend/.env.production

# Docker
docker-compose.override.yml
```

---

## 2. 백엔드 환경 변수

### 2.1 application.yml (공통 — Git 추적)
```yaml
spring:
  application:
    name: smartflow

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate          # 운영: validate, 로컬: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

  mvc:
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS}

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 3600        # 1시간 (초)
  refresh-token-expiry: 604800     # 7일 (초)

google:
  oauth:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-uri: ${GOOGLE_REDIRECT_URI}
    token-uri: https://oauth2.googleapis.com/token
    userinfo-uri: https://www.googleapis.com/oauth2/v3/userinfo

anthropic:
  api:
    key: ${ANTHROPIC_API_KEY}
    model: claude-sonnet-4-6
    max-tokens: 4096
    timeout: 30                    # 초

# mcp: (미사용 — Tool Use 방식으로 전환됨)
#   Google API는 GoogleToolExecutor에서 직접 호출
#   Calendar/Gmail/Drive REST API 타임아웃은 GoogleToolExecutor 내부에서 10초로 설정

encryption:
  secret-key: ${ENCRYPTION_SECRET_KEY}  # Google Token 암호화용 AES 키
```

### 2.2 application-local.yml (로컬 전용 — Git 무시)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update             # 로컬에서는 자동 생성
    show-sql: true

  data:
    redis:
      host: localhost
      port: 6379

logging:
  level:
    com.smartflow: DEBUG
    org.hibernate.SQL: DEBUG
```

### 2.3 백엔드 환경 변수 전체 목록

| 변수명 | 예시값 | 필수 | 설명 |
|--------|--------|------|------|
| `DB_URL` | `jdbc:mysql://localhost:3306/smartflow?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8` | Y | MySQL JDBC URL |
| `DB_USERNAME` | `smartflow` | Y | DB 접속 계정 |
| `DB_PASSWORD` | `your_db_password` | Y | DB 비밀번호 |
| `REDIS_HOST` | `localhost` | Y | Redis 호스트 |
| `REDIS_PORT` | `6379` | N | Redis 포트 (기본 6379) |
| `REDIS_PASSWORD` | `` | N | Redis 비밀번호 (없으면 공백) |
| `JWT_SECRET` | `your_jwt_secret_min_32chars` | Y | JWT 서명 키 (최소 32자) |
| `GOOGLE_CLIENT_ID` | `123456-xxx.apps.googleusercontent.com` | Y | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | `GOCSPX-xxx` | Y | Google OAuth Client Secret |
| `GOOGLE_REDIRECT_URI` | `http://localhost:5173/oauth/callback` | Y | OAuth 리다이렉트 URI |
| `ANTHROPIC_API_KEY` | `sk-ant-api03-xxx` | Y | Anthropic API 키 |
| `ENCRYPTION_SECRET_KEY` | `32자_AES_키` | Y | Google Token 암호화 AES-256 키 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Y | 허용 Origin (콤마 구분) |

### 2.4 .env.local 샘플 (백엔드)
```bash
# backend/.env.local
DB_URL=jdbc:mysql://localhost:3306/smartflow?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=password123

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=smartflow-local-jwt-secret-key-32chars

GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-your-secret-here
GOOGLE_REDIRECT_URI=http://localhost:5173/oauth/callback

ANTHROPIC_API_KEY=sk-ant-api03-your-key-here

ENCRYPTION_SECRET_KEY=your-32-char-aes-256-secret-key!

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

---

## 3. 프론트엔드 환경 변수

### 3.1 .env (공통 기본값 — Git 추적)
```bash
# frontend/.env
VITE_APP_NAME=SmartFlow
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws-smartflow
VITE_GOOGLE_REDIRECT_URI=http://localhost:5173/oauth/callback
```

### 3.2 .env.production (운영 — Git 무시)
```bash
# frontend/.env.production
VITE_API_BASE_URL=https://api.smartflow.example.com
VITE_WS_URL=wss://api.smartflow.example.com/ws-smartflow
VITE_GOOGLE_REDIRECT_URI=https://smartflow.example.com/oauth/callback
```

### 3.3 프론트엔드 환경 변수 전체 목록

| 변수명 | 로컬값 | 설명 |
|--------|--------|------|
| `VITE_APP_NAME` | `SmartFlow` | 앱 표시 이름 |
| `VITE_API_BASE_URL` | `http://localhost:8080` | 백엔드 REST API Base URL |
| `VITE_WS_URL` | `ws://localhost:8080/ws-smartflow` | WebSocket 엔드포인트 |
| `VITE_GOOGLE_REDIRECT_URI` | `http://localhost:5173/oauth/callback` | Google OAuth 콜백 URI |

> Vite 환경 변수는 반드시 `VITE_` 접두사를 붙여야 클라이언트 코드에서 `import.meta.env.VITE_*`로 접근 가능하다.

### 3.4 Vue.js에서 환경 변수 사용
```javascript
// api/axios.js
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
})

// composables/useWebSocket.js
const wsUrl = import.meta.env.VITE_WS_URL
const stompClient = new Client({ brokerURL: wsUrl })
```

---

## 4. Docker Compose 환경 설정

### docker-compose.yml (Git 추적)
```yaml
version: '3.9'

services:
  mysql:
    image: mysql:8.0
    container_name: smartflow-mysql
    environment:
      MYSQL_DATABASE: smartflow
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: smartflow-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  mysql_data:
  redis_data:
```

> 로컬 개발 시 MySQL과 Redis만 Docker로 띄우고, Spring Boot와 Vue.js는 IDE에서 직접 실행한다.

---

## 5. Google OAuth 설정 가이드

### 5.1 Google Cloud Console 설정 순서
```
1. https://console.cloud.google.com 접속
2. 새 프로젝트 생성: "SmartFlow"
3. API 및 서비스 → 사용자 인증 정보
4. OAuth 2.0 클라이언트 ID 생성
   - 애플리케이션 유형: 웹 애플리케이션
   - 승인된 리디렉션 URI 추가:
     - http://localhost:5173/oauth/callback  (로컬)
     - https://smartflow.example.com/oauth/callback  (운영)
5. Client ID, Client Secret 복사 → .env.local에 저장
```

### 5.2 필요한 OAuth 스코프
```
https://www.googleapis.com/auth/gmail.readonly
https://www.googleapis.com/auth/gmail.compose
https://www.googleapis.com/auth/calendar
https://www.googleapis.com/auth/drive.readonly
https://www.googleapis.com/auth/userinfo.email
https://www.googleapis.com/auth/userinfo.profile
```

### 5.3 Spring Boot Google OAuth URL 생성
```java
// GoogleOAuthClient.java
public String buildAuthorizationUrl(String state) {
    return UriComponentsBuilder
        .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
        .queryParam("client_id", googleClientId)
        .queryParam("redirect_uri", googleRedirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", String.join(" ", REQUIRED_SCOPES))
        .queryParam("access_type", "offline")   // Refresh Token 받기 위해 필수
        .queryParam("prompt", "consent")         // 매번 동의 화면 표시 (Refresh Token 보장)
        .queryParam("state", state)
        .build()
        .toUriString();
}
```

---

## 6. 로컬 개발 환경 시작 순서

```bash
# 1. 인프라 실행
cd smartflow
docker-compose up -d

# 2. 백엔드 실행 (IntelliJ 또는 터미널)
cd backend
# .env.local 파일이 있는지 확인
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 프론트엔드 실행
cd frontend
npm install
npm run dev

# 4. 브라우저 접속
# http://localhost:5173
```

### 환경 확인 체크리스트
```
[ ] docker-compose up -d 성공 (MySQL + Redis)
[ ] MySQL: smartflow 데이터베이스 생성 확인
[ ] backend/.env.local 파일 존재
[ ] GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET 설정
[ ] ANTHROPIC_API_KEY 설정
[ ] Spring Boot 8080 포트 기동 확인
[ ] frontend/.env 파일 존재
[ ] Vue.js 5173 포트 기동 확인
[ ] http://localhost:5173/login 접속 후 Google 로그인 테스트
```

---

## 7. 환경 변수 보안 원칙

| 원칙 | 내용 |
|------|------|
| Git 절대 커밋 금지 | `.env.local`, `application-local.yml` 등 시크릿 파일은 `.gitignore`에 반드시 등록 |
| 최소 권한 | Google OAuth 스코프는 필요한 것만 요청 |
| 키 로테이션 | `JWT_SECRET`, `ENCRYPTION_SECRET_KEY`는 정기적으로 교체 |
| 로그 마스킹 | API Key, Token 등은 로그에 절대 출력 금지 (`@JsonIgnore`, 로거 필터) |
| DB 비밀번호 | 운영 환경에서 루트 계정 사용 금지, 전용 계정 생성 |
