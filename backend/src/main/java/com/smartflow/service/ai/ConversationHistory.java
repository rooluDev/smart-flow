package com.smartflow.service.ai;

import com.smartflow.domain.Message;
import com.smartflow.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConversationHistory {

    private static final int MAX_HISTORY = 20;
    private final MessageService messageService;

    public List<Map<String, Object>> build(Long conversationId) {
        List<Message> history = messageService.getHistory(conversationId);
        int start = Math.max(0, history.size() - MAX_HISTORY);
        return history.subList(start, history.size()).stream()
                .map(m -> Map.<String, Object>of(
                        "role", m.getRole().name().toLowerCase(),
                        "content", m.getContent()
                ))
                .toList();
    }
}
