package com.smartflow.service.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class McpOrchestrator {

    private final McpServerRegistry registry;
    private final GoogleMcpTokenManager tokenManager;

    /**
     * Anthropic API 요청에 포함할 mcp_servers 배열 조립.
     * authorization_token은 MCP 서버로 요청 시 Bearer 헤더로 자동 전달됨.
     */
    public List<Map<String, Object>> buildMcpServers(Long userId) {
        String token;
        try {
            token = tokenManager.getAccessToken(userId);
        } catch (Exception e) {
            log.warn("Google token unavailable for user {}, MCP disabled: {}", userId, e.getMessage());
            return List.of();
        }

        return List.of(
                buildServerEntry(McpServerRegistry.GMAIL_SERVER_NAME,    registry.getGmailUrl(),    token),
                buildServerEntry(McpServerRegistry.CALENDAR_SERVER_NAME, registry.getCalendarUrl(), token),
                buildServerEntry(McpServerRegistry.DRIVE_SERVER_NAME,    registry.getDriveUrl(),    token)
        );
    }

    private Map<String, Object> buildServerEntry(String name, String url, String token) {
        return Map.of(
                "type",                "url",
                "url",                 url,
                "name",                name,
                "authorization_token", token
        );
    }

    /**
     * SSE tool_use 이벤트에서 MCP 서버 이름 추출.
     * Anthropic은 "mcp__{server_name}__{tool_name}" 형식을 사용.
     */
    public String extractMcpServerName(String toolName) {
        if (toolName == null) return null;
        if (toolName.startsWith("mcp__")) {
            String[] parts = toolName.split("__", 3);
            if (parts.length >= 2) return parts[1];
        }
        // 직접 매핑 (fallback)
        if (toolName.contains("gmail") || toolName.contains("mail") || toolName.contains("message")) {
            return McpServerRegistry.GMAIL_SERVER_NAME;
        }
        if (toolName.contains("calendar") || toolName.contains("event")) {
            return McpServerRegistry.CALENDAR_SERVER_NAME;
        }
        if (toolName.contains("drive") || toolName.contains("file") || toolName.contains("document")) {
            return McpServerRegistry.DRIVE_SERVER_NAME;
        }
        return toolName;
    }
}
