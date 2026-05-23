package com.smartflow.websocket;

import com.smartflow.dto.response.StreamChunkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageBroker {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(Long conversationId, StreamChunkResponse chunk) {
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, chunk);
    }

    public void broadcastChunk(Long conversationId, String content) {
        broadcast(conversationId, StreamChunkResponse.chunk(content));
    }

    public void broadcastMcpCall(Long conversationId, String mcpName) {
        broadcast(conversationId, StreamChunkResponse.mcpCall(mcpName));
    }

    public void broadcastDone(Long conversationId, Long messageId) {
        broadcast(conversationId, StreamChunkResponse.done(messageId));
    }

    public void broadcastError(Long conversationId, String errorMessage) {
        broadcast(conversationId, StreamChunkResponse.error(errorMessage));
    }
}
