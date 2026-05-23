package com.smartflow.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartflow.domain.Conversation;
import com.smartflow.domain.Message;
import com.smartflow.exception.ErrorCode;
import com.smartflow.service.ConversationService;
import com.smartflow.service.MessageService;
import com.smartflow.service.mcp.GoogleMcpTokenManager;
import com.smartflow.websocket.ChatMessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeService {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.model}")
    private String model;

    @Value("${anthropic.api.max-tokens}")
    private int maxTokens;

    @Value("${anthropic.api.timeout}")
    private int timeoutSeconds;

    private final ConversationHistory conversationHistory;
    private final ChatMessageBroker messageBroker;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final GoogleMcpTokenManager tokenManager;
    private final GoogleToolExecutor googleToolExecutor;
    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder()
            .baseUrl(ANTHROPIC_API_URL)
            .build();

    // ── 진입점 ───────────────────────────────────────────────────────────────

    @Async
    public void streamResponse(Long conversationId, Long userId) {
        Conversation conversation;
        try {
            conversation = conversationService.findByIdAndUser(conversationId, userId);
        } catch (Exception e) {
            messageBroker.broadcastError(conversationId, ErrorCode.CONV_001.getMessage());
            return;
        }

        List<Map<String, Object>> messages = conversationHistory.build(conversationId);
        if (messages.isEmpty()) {
            messageBroker.broadcastError(conversationId, "메시지가 없습니다.");
            return;
        }

        String accessToken = null;
        try {
            accessToken = tokenManager.getAccessToken(userId);
        } catch (Exception e) {
            log.warn("Google token unavailable for user {}, tools disabled", userId);
        }

        AtomicReference<StringBuilder> contentBuilder   = new AtomicReference<>(new StringBuilder());
        AtomicReference<String>        toolsUsedJson    = new AtomicReference<>(null);
        AtomicBoolean                  done             = new AtomicBoolean(false);
        int                            originalMsgCount = messages.size();

        executeStreamRound(conversationId, conversation, userId,
                new ArrayList<>(messages), accessToken,
                contentBuilder, toolsUsedJson, done, originalMsgCount);
    }

    // ── 스트리밍 라운드 ───────────────────────────────────────────────────────

    private void executeStreamRound(
            Long conversationId,
            Conversation conversation,
            Long userId,
            List<Map<String, Object>> messages,
            String accessToken,
            AtomicReference<StringBuilder> contentBuilder,
            AtomicReference<String> toolsUsedJson,
            AtomicBoolean done,
            int originalMsgCount
    ) {
        // 라운드별 로컬 상태
        AtomicReference<String>        currentToolId          = new AtomicReference<>(null);
        AtomicReference<String>        currentToolName        = new AtomicReference<>(null);
        AtomicReference<StringBuilder> currentToolInputBuf    = new AtomicReference<>(null);
        AtomicReference<StringBuilder> roundTextBuf           = new AtomicReference<>(new StringBuilder());
        List<Map<String, Object>>      roundToolUses          = new ArrayList<>();
        AtomicReference<String>        stopReason             = new AtomicReference<>(null);

        Map<String, Object> requestBody = buildRequestBody(messages, accessToken != null);

        try {
            webClient.post()
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnNext(line -> processLine(
                            line, conversationId,
                            contentBuilder, roundTextBuf,
                            currentToolId, currentToolName, currentToolInputBuf,
                            roundToolUses, stopReason, toolsUsedJson
                    ))
                    .doOnComplete(() -> {
                        if (done.get()) return;

                        if ("tool_use".equals(stopReason.get()) && !roundToolUses.isEmpty()) {
                            // 도구 실행 후 다음 라운드
                            Mono.fromRunnable(() -> {
                                try {
                                    List<Map<String, Object>> next = buildNextMessages(
                                            messages, roundTextBuf.get().toString(), roundToolUses, accessToken, toolsUsedJson);
                                    executeStreamRound(conversationId, conversation, userId,
                                            next, accessToken, contentBuilder, toolsUsedJson, done, originalMsgCount);
                                } catch (Exception e) {
                                    if (!done.get()) {
                                        log.error("Tool execution failed for conversation {}", conversationId, e);
                                        messageBroker.broadcastError(conversationId, ErrorCode.AI_001.getMessage());
                                    }
                                }
                            }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        } else {
                            finalizeStream(conversationId, conversation, userId,
                                    contentBuilder, toolsUsedJson, done, originalMsgCount);
                        }
                    })
                    .doOnError(e -> {
                        if (!done.get()) handleError(conversationId, e);
                    })
                    .subscribe();
        } catch (WebClientResponseException e) {
            if (!done.get()) handleHttpError(conversationId, e);
        } catch (Exception e) {
            if (!done.get()) {
                log.error("Unexpected error during Claude streaming for conversation {}", conversationId, e);
                messageBroker.broadcastError(conversationId, ErrorCode.AI_001.getMessage());
            }
        }
    }

    // ── SSE 라인 처리 ─────────────────────────────────────────────────────────

    private void processLine(
            String line,
            Long conversationId,
            AtomicReference<StringBuilder> contentBuilder,
            AtomicReference<StringBuilder> roundTextBuf,
            AtomicReference<String> currentToolId,
            AtomicReference<String> currentToolName,
            AtomicReference<StringBuilder> currentToolInputBuf,
            List<Map<String, Object>> roundToolUses,
            AtomicReference<String> stopReason,
            AtomicReference<String> toolsUsedJson
    ) {
        // Spring SSE 코덱이 "data: " 접두어를 제거할 수도 있어서 양쪽 모두 처리
        String json = line.startsWith("data: ") ? line.substring(6).trim() : line.trim();
        if (json.isEmpty() || !json.startsWith("{")) return;

        try {
            JsonNode node = objectMapper.readTree(json);
            switch (node.path("type").asText()) {

                case "content_block_start" -> {
                    JsonNode block     = node.path("content_block");
                    String   blockType = block.path("type").asText();
                    if ("tool_use".equals(blockType)) {
                        currentToolId.set(block.path("id").asText());
                        currentToolName.set(block.path("name").asText());
                        currentToolInputBuf.set(new StringBuilder());
                        messageBroker.broadcastMcpCall(conversationId,
                                getToolDisplayName(block.path("name").asText()));
                        // 사용된 도구 누적
                        String entry    = "{\"tool\":\"" + block.path("name").asText() + "\"}";
                        String existing = toolsUsedJson.get();
                        toolsUsedJson.set(existing == null ? "[" + entry : existing + "," + entry);
                    }
                }

                case "content_block_delta" -> {
                    JsonNode delta     = node.path("delta");
                    String   deltaType = delta.path("type").asText();
                    if ("text_delta".equals(deltaType)) {
                        String text = delta.path("text").asText();
                        contentBuilder.get().append(text);
                        roundTextBuf.get().append(text);
                        messageBroker.broadcastChunk(conversationId, text);
                    } else if ("input_json_delta".equals(deltaType)) {
                        String partial = delta.path("partial_json").asText();
                        if (currentToolInputBuf.get() != null) {
                            currentToolInputBuf.get().append(partial);
                        }
                    }
                }

                case "content_block_stop" -> {
                    if (currentToolId.get() != null) {
                        Map<String, Object> toolUse = new HashMap<>();
                        toolUse.put("id",        currentToolId.get());
                        toolUse.put("name",       currentToolName.get());
                        toolUse.put("inputJson",  currentToolInputBuf.get() != null
                                ? currentToolInputBuf.get().toString() : "{}");
                        roundToolUses.add(toolUse);
                        currentToolId.set(null);
                        currentToolName.set(null);
                        currentToolInputBuf.set(null);
                    }
                }

                case "message_delta" -> {
                    String reason = node.path("delta").path("stop_reason").asText();
                    if (!reason.isEmpty() && !"null".equals(reason)) {
                        stopReason.set(reason);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("SSE parse error: {}", e.getMessage());
        }
    }

    // ── 도구 실행 후 다음 메시지 빌드 ──────────────────────────────────────────

    private List<Map<String, Object>> buildNextMessages(
            List<Map<String, Object>> prevMessages,
            String roundText,
            List<Map<String, Object>> roundToolUses,
            String accessToken,
            AtomicReference<String> toolsUsedJson
    ) {
        List<Object>             assistantContent = new ArrayList<>();
        List<Map<String, Object>> toolResults     = new ArrayList<>();

        if (!roundText.isEmpty()) {
            assistantContent.add(Map.of("type", "text", "text", roundText));
        }

        for (Map<String, Object> toolUse : roundToolUses) {
            String toolName  = (String) toolUse.get("name");
            String inputJson = (String) toolUse.get("inputJson");
            String toolId    = (String) toolUse.get("id");

            JsonNode inputNode;
            try {
                inputNode = objectMapper.readTree(inputJson);
            } catch (Exception e) {
                log.warn("Invalid tool input JSON for {}: {}", toolName, inputJson);
                inputNode = objectMapper.createObjectNode();
            }

            assistantContent.add(Map.of(
                    "type",  "tool_use",
                    "id",    toolId,
                    "name",  toolName,
                    "input", inputNode
            ));

            String result;
            boolean isError = false;
            try {
                result = googleToolExecutor.execute(toolName, inputNode, accessToken);
                log.debug("Tool '{}' executed, result length={}", toolName, result.length());
            } catch (Exception e) {
                log.warn("Tool '{}' failed: {}", toolName, e.getMessage());
                result  = "{\"error\": \"Tool execution failed\"}";
                isError = true;
            }

            Map<String, Object> toolResult = new HashMap<>();
            toolResult.put("type",        "tool_result");
            toolResult.put("tool_use_id", toolId);
            toolResult.put("content",     result);
            if (isError) toolResult.put("is_error", true);
            toolResults.add(toolResult);
        }

        List<Map<String, Object>> next = new ArrayList<>(prevMessages);
        if (!assistantContent.isEmpty()) {
            next.add(Map.of("role", "assistant", "content", assistantContent));
        }
        if (!toolResults.isEmpty()) {
            next.add(Map.of("role", "user", "content", toolResults));
        }
        return next;
    }

    // ── 스트리밍 완료 처리 ────────────────────────────────────────────────────

    private void finalizeStream(
            Long conversationId,
            Conversation conversation,
            Long userId,
            AtomicReference<StringBuilder> contentBuilder,
            AtomicReference<String> toolsUsedJson,
            AtomicBoolean done,
            int originalMsgCount
    ) {
        done.set(true);
        String fullContent = contentBuilder.get().toString();

        if (!fullContent.isEmpty()) {
            String toolsJson = toolsUsedJson.get();
            if (toolsJson != null && !toolsJson.endsWith("]")) toolsJson += "]";

            Message saved = messageService.saveAssistantMessage(conversation, fullContent, toolsJson);
            messageBroker.broadcastDone(conversationId, saved.getId());

            if (originalMsgCount == 1) {
                final String content = fullContent;
                Mono.fromRunnable(() -> autoGenerateTitle(conversation, userId, content))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe();
            }
        } else {
            log.warn("Stream completed with empty content for conversation {}", conversationId);
            messageBroker.broadcastDone(conversationId, null);
        }
    }

    // ── 요청 바디 빌드 ────────────────────────────────────────────────────────

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> messages, boolean includeTools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model",      model);
        body.put("max_tokens", maxTokens);
        body.put("system",     buildSystemPrompt());
        body.put("messages",   messages);
        body.put("stream",     true);
        if (includeTools) {
            body.put("tools", buildToolDefinitions());
        }
        return body;
    }

    private String buildSystemPrompt() {
        return "당신은 SmartFlow AI 어시스턴트입니다.\n"
                + "Gmail, Google Calendar, Google Drive를 통해 사용자의 업무를 도와줍니다.\n"
                + "한국어로 친절하고 명확하게 응답하세요.\n"
                + "오늘 날짜: " + LocalDate.now(ZoneId.of("Asia/Seoul")) + " (한국 표준시 기준)";
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        return List.of(
                Map.of(
                        "name",        "get_calendar_events",
                        "description", "Google Calendar에서 특정 기간의 일정을 조회합니다. 오늘/이번주/다음주 일정 등을 물어볼 때 사용하세요.",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "start_date", Map.of("type", "string",
                                                "description", "조회 시작 날짜 (YYYY-MM-DD 형식)"),
                                        "end_date",   Map.of("type", "string",
                                                "description", "조회 종료 날짜 (YYYY-MM-DD 형식). 하루치는 start_date와 동일하게.")
                                ),
                                "required", List.of("start_date", "end_date")
                        )
                ),
                Map.of(
                        "name",        "get_gmail_messages",
                        "description", "Gmail에서 메일을 검색하고 조회합니다. 받은 메일, 읽지 않은 메일, 특정 발신자 메일 등을 물어볼 때 사용하세요.",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query",       Map.of("type", "string",
                                                "description", "Gmail 검색 쿼리 (예: is:unread, from:user@example.com, subject:회의). 기본값: is:unread"),
                                        "max_results", Map.of("type", "integer",
                                                "description", "최대 조회 개수 (기본 10, 최대 20)")
                                )
                        )
                ),
                Map.of(
                        "name",        "get_drive_files",
                        "description", "Google Drive에서 최근 수정된 파일 목록을 조회합니다.",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "max_results", Map.of("type", "integer",
                                                "description", "최대 조회 개수 (기본 10, 최대 20)")
                                )
                        )
                ),
                Map.of(
                        "name",        "send_gmail",
                        "description", "Gmail로 이메일을 발송합니다.",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "to",      Map.of("type", "string",  "description", "수신자 이메일 주소"),
                                        "subject", Map.of("type", "string",  "description", "메일 제목"),
                                        "body",    Map.of("type", "string",  "description", "메일 본문")
                                ),
                                "required", List.of("to", "subject", "body")
                        )
                )
        );
    }

    private String getToolDisplayName(String toolName) {
        return switch (toolName) {
            case "get_calendar_events" -> "Google Calendar";
            case "get_gmail_messages"  -> "Gmail";
            case "get_drive_files"     -> "Google Drive";
            case "send_gmail"          -> "Gmail 발송";
            default                    -> toolName;
        };
    }

    // ── 제목 자동 생성 ────────────────────────────────────────────────────────

    private void autoGenerateTitle(Conversation conversation, Long userId, String firstUserContent) {
        try {
            String title = generateTitle(firstUserContent);
            if (title != null && !title.isBlank()) {
                conversationService.updateTitle(conversation.getId(), userId, title);
            }
        } catch (Exception e) {
            log.warn("Auto title generation failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String generateTitle(String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model",      model);
        body.put("max_tokens", 30);
        body.put("system",     "대화의 제목을 한국어로 10자 이내로 생성하세요. 제목만 응답하고 다른 텍스트는 포함하지 마세요.");
        body.put("messages",   List.of(Map.of("role", "user", "content", userMessage)));

        try {
            Map<String, Object> response = webClient.post()
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null) {
                List<Map<?, ?>> content = (List<Map<?, ?>>) response.get("content");
                if (content != null && !content.isEmpty()) {
                    return String.valueOf(content.get(0).get("text")).trim();
                }
            }
        } catch (Exception e) {
            log.warn("Title generation API call failed", e);
        }
        return null;
    }

    // ── 에러 처리 ─────────────────────────────────────────────────────────────

    private void handleError(Long conversationId, Throwable e) {
        if (e instanceof java.util.concurrent.TimeoutException) {
            messageBroker.broadcastError(conversationId, ErrorCode.AI_002.getMessage());
        } else if (e instanceof WebClientResponseException we) {
            handleHttpError(conversationId, we);
        } else {
            log.error("Streaming error for conversation {}", conversationId, e);
            messageBroker.broadcastError(conversationId, ErrorCode.AI_001.getMessage());
        }
    }

    private void handleHttpError(Long conversationId, WebClientResponseException e) {
        log.warn("Anthropic HTTP error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
        int status = e.getStatusCode().value();
        if (status == 429) {
            messageBroker.broadcastError(conversationId, ErrorCode.AI_004.getMessage());
        } else if (status >= 500) {
            messageBroker.broadcastError(conversationId, ErrorCode.AI_003.getMessage());
        } else {
            messageBroker.broadcastError(conversationId, ErrorCode.AI_001.getMessage());
        }
    }
}
