# SmartFlow — Product Requirements Document (PRD)

> 버전: 1.0.0  
> 작성일: 2026-04-18  
> 상태: Draft

---

## 1. 프로젝트 개요

### 1.1 제품명
**SmartFlow** — AI 업무 비서 플랫폼

### 1.2 한 줄 정의
Gmail · Google Calendar · Google Drive를 Claude AI가 자연어로 통합 제어하는 개인 업무 자동화 플랫폼

### 1.3 배경 및 목적
업무 중 이메일 확인, 일정 관리, 문서 검색은 매일 반복되지만 각 도구를 개별적으로 열고 닫는 컨텍스트 전환 비용이 크다.
SmartFlow는 하나의 채팅 인터페이스에서 자연어로 모든 Google Workspace 도구를 제어할 수 있게 함으로써 이 문제를 해결한다.

### 1.4 목표 사용자
- 1차 타겟: Google Workspace를 일상적으로 사용하는 1인 개발자 / 프리랜서
- 2차 타겟: 반복적인 이메일·일정 처리가 많은 소규모 팀원

---

## 2. 핵심 기능 (Core Features)

### F-01. AI 채팅 인터페이스
- 사용자가 자연어로 업무 지시를 입력한다
- Claude AI가 의도를 파악하고 적절한 MCP Tool을 자동 선택하여 실행한다
- 응답은 스트리밍으로 실시간 출력된다
- WebSocket(STOMP) 기반으로 연결을 유지한다
- 대화는 세션 단위로 관리되며 히스토리가 보존된다

### F-02. Gmail 연동
- 받은 편지함의 최근 이메일 목록을 조회한다
- 특정 이메일의 본문을 읽고 AI가 요약한다
- 사용자 지시에 따라 답장 초안을 생성한다 (전송은 사용자가 직접 확인 후 수행)
- 특정 조건(발신자, 키워드, 날짜)으로 이메일을 검색한다
- 읽지 않은 이메일 수를 대시보드에 표시한다

### F-03. Google Calendar 연동
- 오늘 / 이번 주 일정을 조회한다
- 자연어 지시로 새 일정을 생성한다 (예: "내일 오후 2시에 팀 미팅 잡아줘")
- 기존 일정을 수정하거나 삭제한다
- 특정 기간의 일정 목록을 요약한다
- 다가오는 일정을 대시보드에 표시한다

### F-04. Google Drive 연동
- 파일명 / 내용 키워드로 문서를 검색한다
- 문서 내용을 읽고 AI가 요약한다
- 최근 수정한 파일 목록을 조회한다
- 파일의 공유 링크를 조회한다

### F-05. 대화 관리
- 대화를 새로 시작하거나 이전 대화를 불러온다
- 대화 목록을 사이드바에 표시한다
- 대화에 제목을 자동 생성한다 (첫 메시지 기반)
- 대화를 삭제한다

### F-06. 사용자 인증
- Google OAuth2 로그인 (MCP 연동 권한 포함)
- JWT 기반 세션 관리 (Access Token + Refresh Token)
- 로그아웃

### F-07. 대시보드
- 읽지 않은 이메일 수
- 오늘의 일정 목록
- 최근 수정한 Drive 파일 목록
- 채팅 입력창 (빠른 접근)

---

## 3. 비기능 요구사항

### 3.1 성능
- AI 응답 첫 토큰 도달 시간: 3초 이내
- 대시보드 초기 로딩: 2초 이내
- MCP API 호출 타임아웃: 10초

### 3.2 보안
- 모든 API 엔드포인트는 JWT 인증 필수
- Google OAuth 토큰은 서버에서만 보관 (프론트엔드 노출 금지)
- HTTPS 필수 (로컬 개발 환경 제외)
- CORS 설정: 허용된 Origin만 접근 가능

### 3.3 확장성
- MCP 연동 대상은 인터페이스 기반으로 설계하여 추후 Slack, Notion 등 추가 가능
- AI 모델은 설정값으로 교체 가능하도록 추상화

### 3.4 가용성
- MVP 기준 단일 서버 구성 (고가용성은 2차 범위)

---

## 4. 기술 스택

| 영역 | 기술 | 버전 | 선택 이유 |
|------|------|------|-----------|
| Backend Language | Java | 17 | LTS, 기존 역량 활용 |
| Backend Framework | Spring Boot | 3.3.x | 생산성, 풍부한 생태계 |
| ORM | Spring Data JPA | - | 엔티티 관리 간소화 |
| SQL Mapper | MyBatis | - | 복잡한 쿼리 처리 |
| Database | MySQL | 8.0 | 기존 역량 활용 |
| Cache / Session | Redis | 7.x | WebSocket 세션, 토큰 저장 |
| Realtime | WebSocket (STOMP) | - | AI 스트리밍 응답 전달 |
| AI | Anthropic Claude API | claude-sonnet-4-6 | Tool Use 지원 |
| Google 연동 | Gmail / Calendar / Drive REST API | - | 백엔드에서 직접 호출 |
| Auth | Spring Security + JWT | - | 표준 인증 구조 |
| Frontend | Vue.js 3 + Pinia + Vue Router | 3.x | 기존 역량 활용 |
| HTTP Client | Axios | - | REST API 통신 |
| Build Tool (FE) | Vite | - | 빠른 개발 서버 |
| Container | Docker Compose | - | 로컬 환경 통일 |

---

## 5. Tool Use 연동 구조

Spring Boot 백엔드가 Anthropic API 호출 시 `tools` 파라미터로 사용 가능한 도구를 정의한다.
Claude AI가 도구 호출이 필요하다고 판단하면 백엔드가 직접 Google API를 실행하고 결과를 Claude에게 반환한다.

```
[Vue.js] ──WebSocket(STOMP)──▶ [Spring Boot]
                                      │
                                      ▼
                            [Anthropic Claude API]
                            tools 파라미터 포함
                                      │
                          stop_reason: "tool_use"
                                      │
                                      ▼
                            [Spring Boot: GoogleToolExecutor]
                          ┌───────────┼───────────┐
                          ▼           ▼           ▼
                      [Gmail]    [Calendar]   [Drive]
                      REST API   REST API     REST API
                          │           │           │
                          └───────────┴───────────┘
                                      │ tool_result
                                      ▼
                            [Anthropic Claude API]
                            최종 답변 스트리밍
```

| 도구 | 설명 |
|------|------|
| `get_calendar_events` | Google Calendar에서 특정 기간 일정 조회 |
| `get_gmail_messages` | Gmail 메일 검색 및 조회 |
| `get_drive_files` | Google Drive 최근 파일 목록 조회 |
| `send_gmail` | Gmail로 이메일 발송 |

---

## 6. 주요 사용자 시나리오

### S-01. 오늘 업무 브리핑
```
사용자: "오늘 일정이랑 안 읽은 이메일 요약해줘"
→ get_calendar_events: 오늘 일정 조회 (Google Calendar API)
→ get_gmail_messages: 읽지 않은 이메일 조회 (Gmail API)
→ AI: 통합 브리핑 응답 생성
```

### S-02. 이메일 답장 초안 작성
```
사용자: "가장 최근 이메일 읽고 정중한 답장 초안 써줘"
→ get_gmail_messages: 최신 이메일 조회 (Gmail API)
→ AI: 답장 초안 작성 후 응답
```

### S-03. 자연어로 일정 조회
```
사용자: "이번 주 일정 정리해줘"
→ get_calendar_events: 이번 주 일정 조회 (Google Calendar API)
→ AI: 일정 목록 정리 후 응답
```

### S-04. Drive 파일 조회
```
사용자: "최근 수정한 파일 보여줘"
→ get_drive_files: 최근 파일 목록 조회 (Google Drive API)
→ AI: 파일 목록 정리 후 응답
```

### S-05. 이메일 발송
```
사용자: "팀장님께 '내일 회의 참석합니다' 이메일 보내줘"
→ AI: 수신자 및 내용 확인
→ send_gmail: Gmail API로 이메일 발송
→ AI: 발송 완료 확인 응답
```

---

## 7. Out of Scope (MVP 제외 항목)

| 항목 | 이유 |
|------|------|
| 이메일 직접 전송 | 사용자 확인 없는 자동 전송은 보안 리스크 |
| Drive 파일 생성 / 업로드 | 읽기·검색만 MVP 범위 |
| 팀 협업 기능 | 1인 사용자 최적화 우선 |
| 모바일 앱 | 웹 SPA만 구현 |
| Slack / Notion MCP 연동 | 추후 확장 과제 |

---

## 8. 마일스톤

| 단계 | 내용 | 목표 기간 |
|------|------|-----------|
| M1 | 프로젝트 셋업 + Google OAuth 인증 | 1주 |
| M2 | AI 채팅 기본 기능 (WebSocket 스트리밍) | 1주 |
| M3 | Gmail Tool Use 연동 | 1주 |
| M4 | Google Calendar Tool Use 연동 | 1주 |
| M5 | Google Drive Tool Use 연동 + 대시보드 | 1주 |
| M6 | UI 완성 + QA + 배포 준비 | 1주 |

**총 예상 기간: 6주**

---

## 9. 성공 지표

- [ ] 4개 Google 도구 모두 정상 동작 확인 (Calendar / Gmail 조회·발송 / Drive)
- [ ] 자연어 지시 → Tool Use 도구 선택 → Google API 실행 → 응답 end-to-end 동작
- [ ] WebSocket 스트리밍 응답 정상 동작
- [ ] 대화 히스토리 저장 및 불러오기 동작
- [ ] Google OAuth 로그인 정상 동작
- [ ] 대시보드 3개 위젯 모두 데이터 표시
