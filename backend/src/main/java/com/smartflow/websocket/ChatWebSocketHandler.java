package com.smartflow.websocket;

import com.smartflow.dto.request.ChatMessageRequest;
import com.smartflow.exception.ErrorCode;
import com.smartflow.service.ConversationService;
import com.smartflow.service.MessageService;
import com.smartflow.service.ai.ClaudeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ClaudeService claudeService;
    private final ChatMessageBroker messageBroker;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated WebSocket message received");
            return;
        }

        Long userId = ((JwtStompInterceptor.StompPrincipal) principal).userId();
        Long conversationId = request.getConversationId();
        String content = request.getContent();

        if (content == null || content.isBlank()) {
            messageBroker.broadcastError(conversationId, ErrorCode.MSG_001.getMessage());
            return;
        }
        if (content.length() > 5000) {
            messageBroker.broadcastError(conversationId, ErrorCode.MSG_002.getMessage());
            return;
        }

        try {
            var conversation = conversationService.findByIdAndUser(conversationId, userId);
            messageService.saveUserMessage(conversation, content);
            claudeService.streamResponse(conversationId, userId);
        } catch (Exception e) {
            log.error("Error handling chat message", e);
            messageBroker.broadcastError(conversationId, ErrorCode.AI_001.getMessage());
        }
    }
}
