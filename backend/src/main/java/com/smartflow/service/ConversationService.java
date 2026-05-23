package com.smartflow.service;

import com.smartflow.domain.Conversation;
import com.smartflow.domain.User;
import com.smartflow.dto.response.ConversationResponse;
import com.smartflow.exception.ErrorCode;
import com.smartflow.exception.SmartflowException;
import com.smartflow.repository.ConversationRepository;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private static final String CONV_LIST_KEY = "smartflow:conv_list:";
    private static final long CONV_LIST_TTL_MINUTES = 10L;

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public ConversationResponse.CreateResponse create(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SmartflowException(ErrorCode.AUTH_001));
        Conversation conversation = Conversation.builder()
                .user(user)
                .title("새 대화")
                .build();
        Conversation saved = conversationRepository.save(conversation);
        invalidateListCache(userId);
        return ConversationResponse.CreateResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ConversationResponse.ListResponse getList(Long userId, int page, int size) {
        Page<Conversation> result = conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(page, size));
        return ConversationResponse.ListResponse.builder()
                .conversations(result.getContent().stream()
                        .map(ConversationResponse.Summary::from)
                        .toList())
                .totalCount(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getOne(Long conversationId, Long userId) {
        Conversation conversation = findByIdAndUser(conversationId, userId);
        return ConversationResponse.from(conversation);
    }

    @Transactional
    public ConversationResponse.TitleUpdateResponse updateTitle(Long conversationId, Long userId, String title) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new SmartflowException(ErrorCode.CONV_003);
        }
        Conversation conversation = findByIdAndUser(conversationId, userId);
        conversation.setTitle(title);
        conversationRepository.save(conversation);
        invalidateListCache(userId);
        return ConversationResponse.TitleUpdateResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .build();
    }

    @Transactional
    public void delete(Long conversationId, Long userId) {
        Conversation conversation = findByIdAndUser(conversationId, userId);
        conversationRepository.delete(conversation);
        invalidateListCache(userId);
    }

    public Conversation findByIdAndUser(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> {
                    if (conversationRepository.existsById(conversationId)) {
                        return new SmartflowException(ErrorCode.CONV_002);
                    }
                    return new SmartflowException(ErrorCode.CONV_001);
                });
    }

    private void invalidateListCache(Long userId) {
        redisTemplate.delete(CONV_LIST_KEY + userId);
    }
}
