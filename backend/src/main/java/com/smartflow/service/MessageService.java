package com.smartflow.service;

import com.smartflow.domain.Conversation;
import com.smartflow.domain.Message;
import com.smartflow.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message saveUserMessage(Conversation conversation, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(Message.MessageRole.USER)
                .content(content)
                .build();
        return messageRepository.save(message);
    }

    @Transactional
    public Message saveAssistantMessage(Conversation conversation, String content, String mcpToolsUsed) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(Message.MessageRole.ASSISTANT)
                .content(content)
                .mcpToolsUsed(mcpToolsUsed)
                .build();
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getHistory(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
