package com.smartflow.dto.response;

import com.smartflow.domain.Conversation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ConversationResponse {

    private final Long id;
    private final String title;
    private final List<MessageResponse> messages;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ConversationResponse from(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .messages(conversation.getMessages().stream()
                        .map(MessageResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    @Getter
    @Builder
    public static class Summary {
        private final Long id;
        private final String title;
        private final String lastMessage;
        private final String lastMessageRole;
        private final LocalDateTime updatedAt;

        public static Summary from(Conversation conversation) {
            String lastMsg = null;
            String lastRole = null;
            if (!conversation.getMessages().isEmpty()) {
                var last = conversation.getMessages().get(conversation.getMessages().size() - 1);
                lastMsg = last.getContent().length() > 100
                        ? last.getContent().substring(0, 100) + "..."
                        : last.getContent();
                lastRole = last.getRole().name();
            }
            return Summary.builder()
                    .id(conversation.getId())
                    .title(conversation.getTitle())
                    .lastMessage(lastMsg)
                    .lastMessageRole(lastRole)
                    .updatedAt(conversation.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private final Long id;
        private final String title;
        private final LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class TitleUpdateResponse {
        private final Long id;
        private final String title;
    }

    @Getter
    @Builder
    public static class ListResponse {
        private final List<Summary> conversations;
        private final long totalCount;
        private final boolean hasNext;
    }
}
