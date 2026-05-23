package com.smartflow.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleToolExecutor {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.create();

    public String execute(String toolName, JsonNode input, String accessToken) {
        try {
            return switch (toolName) {
                case "get_calendar_events" -> getCalendarEvents(input, accessToken);
                case "get_gmail_messages"  -> getGmailMessages(input, accessToken);
                case "get_drive_files"     -> getDriveFiles(input, accessToken);
                case "send_gmail"          -> sendGmail(input, accessToken);
                default -> "{\"error\": \"Unknown tool: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.warn("Tool '{}' execution failed: {}", toolName, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ── Google Calendar ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String getCalendarEvents(JsonNode input, String accessToken) {
        String startDate = input.path("start_date").asText();
        String endDate   = input.path("end_date").asText();

        // Instant(UTC)으로 변환해서 "Z" suffix 사용 → URL에서 "+" 인코딩 문제 방지
        String timeMin = LocalDate.parse(startDate).atStartOfDay(KST).toInstant().toString();
        String timeMax = LocalDate.parse(endDate).plusDays(1).atStartOfDay(KST).toInstant().toString();

        Map<String, Object> response = webClient.get()
                .uri(b -> b.scheme("https").host("www.googleapis.com")
                        .path("/calendar/v3/calendars/primary/events")
                        .queryParam("timeMin", timeMin)
                        .queryParam("timeMax", timeMax)
                        .queryParam("singleEvents", "true")
                        .queryParam("orderBy", "startTime")
                        .queryParam("maxResults", "20")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .block();

        if (response == null) return "[]";
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) response.getOrDefault("items", List.of());

        List<Map<String, Object>> events = items.stream().map(item -> {
            Map<String, Object> start = (Map<String, Object>) item.get("start");
            Map<String, Object> end   = (Map<String, Object>) item.get("end");
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("title",    item.getOrDefault("summary", "(제목 없음)"));
            e.put("start",    start != null ? start.getOrDefault("dateTime", start.getOrDefault("date", "")) : "");
            e.put("end",      end   != null ? end.getOrDefault("dateTime",   end.getOrDefault("date", ""))   : "");
            e.put("location", item.getOrDefault("location", ""));
            e.put("description", item.getOrDefault("description", ""));
            return e;
        }).toList();

        return toJson(events);
    }

    // ── Gmail ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String getGmailMessages(JsonNode input, String accessToken) {
        String query      = input.path("query").asText("is:unread");
        int    maxResults = Math.min(input.path("max_results").asInt(10), 20);

        // 1) message ID 목록
        Map<String, Object> listResponse = webClient.get()
                .uri(b -> b.scheme("https").host("www.googleapis.com")
                        .path("/gmail/v1/users/me/messages")
                        .queryParam("maxResults", maxResults)
                        .queryParam("q", query)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .block();

        if (listResponse == null) return "[]";
        List<Map<String, Object>> refs =
                (List<Map<String, Object>>) listResponse.getOrDefault("messages", List.of());

        // 2) 메시지 상세(최대 5건)
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> ref : refs.subList(0, Math.min(5, refs.size()))) {
            String id = (String) ref.get("id");
            try {
                Map<String, Object> msg = webClient.get()
                        .uri(b -> b.scheme("https").host("www.googleapis.com")
                                .path("/gmail/v1/users/me/messages/" + id)
                                .queryParam("format", "metadata")
                                .queryParam("metadataHeaders", "Subject")
                                .queryParam("metadataHeaders", "From")
                                .queryParam("metadataHeaders", "Date")
                                .build())
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(TIMEOUT)
                        .block();

                if (msg != null) {
                    List<Map<String, Object>> headers = (List<Map<String, Object>>)
                            ((Map<?, ?>) msg.get("payload")).get("headers");
                    Map<String, String> h = new HashMap<>();
                    if (headers != null) {
                        headers.forEach(hdr -> h.put((String) hdr.get("name"), (String) hdr.get("value")));
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",      id);
                    m.put("subject", h.getOrDefault("Subject", "(제목 없음)"));
                    m.put("from",    h.getOrDefault("From", ""));
                    m.put("date",    h.getOrDefault("Date", ""));
                    m.put("snippet", msg.getOrDefault("snippet", ""));
                    messages.add(m);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch gmail message {}: {}", id, e.getMessage());
            }
        }
        return toJson(messages);
    }

    // ── Google Drive ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String getDriveFiles(JsonNode input, String accessToken) {
        int maxResults = Math.min(input.path("max_results").asInt(10), 20);

        Map<String, Object> response = webClient.get()
                .uri(b -> b.scheme("https").host("www.googleapis.com")
                        .path("/drive/v3/files")
                        .queryParam("orderBy", "modifiedTime desc")
                        .queryParam("pageSize", maxResults)
                        .queryParam("q", "trashed=false")
                        .queryParam("fields", "files(id,name,mimeType,modifiedTime,webViewLink)")
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .block();

        if (response == null) return "[]";
        List<Map<String, Object>> files =
                (List<Map<String, Object>>) response.getOrDefault("files", List.of());

        List<Map<String, Object>> result = files.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",         f.getOrDefault("name", ""));
            m.put("mimeType",     f.getOrDefault("mimeType", ""));
            m.put("modifiedTime", f.getOrDefault("modifiedTime", ""));
            m.put("webViewLink",  f.getOrDefault("webViewLink", ""));
            return m;
        }).toList();

        return toJson(result);
    }

    // ── Gmail 발송 ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String sendGmail(JsonNode input, String accessToken) {
        String to      = input.path("to").asText();
        String subject = input.path("subject").asText();
        String body    = input.path("body").asText();

        String raw = "To: " + to + "\r\n"
                + "Subject: =?UTF-8?B?" + Base64.getEncoder()
                        .encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));

        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> response = webClient.post()
                .uri("https://www.googleapis.com/gmail/v1/users/me/messages/send")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("raw", encoded))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .block();

        if (response != null && response.containsKey("id")) {
            return "{\"success\": true, \"messageId\": \"" + response.get("id") + "\"}";
        }
        return "{\"success\": false}";
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
