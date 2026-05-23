package com.smartflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartflow.dto.response.ConversationSummaryDto;
import com.smartflow.dto.response.DashboardResponse;
import com.smartflow.mapper.DashboardMapper;
import com.smartflow.service.mcp.GoogleMcpTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private static final String GMAIL_API = "https://www.googleapis.com/gmail/v1";
    private static final Duration TIMEOUT    = Duration.ofSeconds(10);
    private static final String CACHE_KEY    = "smartflow:dashboard:";
    private static final long CACHE_TTL_MINUTES = 5L;
    private static final int CONVERSATION_LIMIT = 5;

    private final GoogleMcpTokenManager tokenManager;
    private final DashboardMapper dashboardMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.create();

    public DashboardResponse getDashboard(Long userId) {
        String cacheKey = CACHE_KEY + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, DashboardResponse.class);
            } catch (Exception e) {
                log.warn("Dashboard cache deserialization failed, refetching: {}", e.getMessage());
            }
        }

        DashboardResponse response = buildDashboard(userId);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Dashboard cache write failed: {}", e.getMessage());
        }

        return response;
    }

    public void invalidateCache(Long userId) {
        redisTemplate.delete(CACHE_KEY + userId);
    }

    private DashboardResponse buildDashboard(Long userId) {
        List<ConversationSummaryDto> conversations =
                dashboardMapper.findConversationSummaries(userId, CONVERSATION_LIMIT);

        String accessToken;
        try {
            accessToken = tokenManager.getAccessToken(userId);
        } catch (Exception e) {
            log.warn("Google token unavailable for dashboard, userId={}", userId);
            return emptyDashboard(conversations);
        }

        int unreadCount = fetchGmailUnreadCount(accessToken);
        List<DashboardResponse.CalendarData.Event> todayEvents = fetchTodayCalendarEvents(accessToken);
        List<DashboardResponse.DriveData.File> recentFiles = fetchRecentDriveFiles(accessToken);

        return DashboardResponse.builder()
                .email(DashboardResponse.EmailData.builder()
                        .unreadCount(unreadCount)
                        .build())
                .calendar(DashboardResponse.CalendarData.builder()
                        .todayEvents(todayEvents)
                        .build())
                .drive(DashboardResponse.DriveData.builder()
                        .recentFiles(recentFiles)
                        .build())
                .conversations(conversations)
                .build();
    }

    // ── Gmail ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int fetchGmailUnreadCount(String accessToken) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(GMAIL_API + "/users/me/labels/INBOX")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
            if (response != null && response.containsKey("messagesUnread")) {
                return ((Number) response.get("messagesUnread")).intValue();
            }
        } catch (Exception e) {
            log.warn("Gmail unread count fetch failed: {}", e.getMessage());
        }
        return 0;
    }

    // ── Google Calendar ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<DashboardResponse.CalendarData.Event> fetchTodayCalendarEvents(String accessToken) {
        try {
            ZoneId zone = ZoneId.of("Asia/Seoul");
            LocalDate today = LocalDate.now(zone);
            // Instant(UTC)으로 변환해서 "Z" suffix 사용 → URL에서 "+" 인코딩 문제 방지
            String timeMin = today.atStartOfDay(zone).toInstant().toString();
            String timeMax = today.plusDays(1).atStartOfDay(zone).toInstant().toString();

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/calendar/v3/calendars/primary/events")
                            .queryParam("timeMin", timeMin)
                            .queryParam("timeMax", timeMax)
                            .queryParam("singleEvents", "true")
                            .queryParam("orderBy", "startTime")
                            .queryParam("maxResults", "10")
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null) return List.of();
            List<Map<String, Object>> items =
                    (List<Map<String, Object>>) response.getOrDefault("items", List.of());
            List<DashboardResponse.CalendarData.Event> events = new ArrayList<>();
            for (Map<String, Object> item : items) {
                events.add(parseCalendarEvent(item));
            }
            return events;
        } catch (Exception e) {
            log.warn("Calendar events fetch failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private DashboardResponse.CalendarData.Event parseCalendarEvent(Map<String, Object> item) {
        String id       = (String) item.get("id");
        String summary  = (String) item.getOrDefault("summary", "(제목 없음)");
        String location = (String) item.get("location");
        Map<String, Object> start = (Map<String, Object>) item.get("start");
        Map<String, Object> end   = (Map<String, Object>) item.get("end");
        return DashboardResponse.CalendarData.Event.builder()
                .id(id).title(summary).location(location)
                .startTime(parseEventDateTime(start))
                .endTime(parseEventDateTime(end))
                .build();
    }

    private LocalDateTime parseEventDateTime(Map<String, Object> map) {
        if (map == null) return null;
        String dateTime = (String) map.get("dateTime");
        if (dateTime != null) return OffsetDateTime.parse(dateTime).toLocalDateTime();
        String date = (String) map.get("date");
        if (date != null) return LocalDate.parse(date).atStartOfDay();
        return null;
    }

    // ── Google Drive ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<DashboardResponse.DriveData.File> fetchRecentDriveFiles(String accessToken) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/drive/v3/files")
                            .queryParam("orderBy", "modifiedTime desc")
                            .queryParam("pageSize", "3")
                            .queryParam("q", "trashed=false")
                            .queryParam("fields", "files(id,name,mimeType,modifiedTime,webViewLink)")
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null) return List.of();
            List<Map<String, Object>> files =
                    (List<Map<String, Object>>) response.getOrDefault("files", List.of());
            List<DashboardResponse.DriveData.File> result = new ArrayList<>();
            for (Map<String, Object> f : files) {
                result.add(parseDriveFile(f));
            }
            return result;
        } catch (Exception e) {
            log.warn("Drive files fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    private DashboardResponse.DriveData.File parseDriveFile(Map<String, Object> f) {
        String modifiedTimeStr = (String) f.get("modifiedTime");
        LocalDateTime modifiedTime = null;
        if (modifiedTimeStr != null) {
            modifiedTime = OffsetDateTime.parse(modifiedTimeStr).toLocalDateTime();
        }
        return DashboardResponse.DriveData.File.builder()
                .id((String) f.get("id"))
                .name((String) f.get("name"))
                .mimeType((String) f.get("mimeType"))
                .modifiedTime(modifiedTime)
                .webViewLink((String) f.get("webViewLink"))
                .build();
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private DashboardResponse emptyDashboard(List<ConversationSummaryDto> conversations) {
        return DashboardResponse.builder()
                .email(DashboardResponse.EmailData.builder().unreadCount(0).build())
                .calendar(DashboardResponse.CalendarData.builder().todayEvents(List.of()).build())
                .drive(DashboardResponse.DriveData.builder().recentFiles(List.of()).build())
                .conversations(conversations)
                .build();
    }
}
