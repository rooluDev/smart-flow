package com.smartflow.dto.response;

import com.smartflow.domain.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private final Long id;
    private final String role;
    private final String content;
    private final String mcpToolsUsed;
    private final LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .mcpToolsUsed(message.getMcpToolsUsed())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
