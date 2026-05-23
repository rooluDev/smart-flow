package com.smartflow.service.mcp;

import com.smartflow.domain.User;
import com.smartflow.exception.ErrorCode;
import com.smartflow.exception.SmartflowException;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleMcpTokenManager {

    private static final String GOOGLE_TOKEN_KEY = "smartflow:google_token:";

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.token-uri}")
    private String tokenUri;

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final WebClient webClient = WebClient.create();

    public String getAccessToken(Long userId) {
        // 1) Redis 캐시에서 우선 조회
        String cached = (String) redisTemplate.opsForHash()
                .get(GOOGLE_TOKEN_KEY + userId, "access_token");
        String expiresAtStr = (String) redisTemplate.opsForHash()
                .get(GOOGLE_TOKEN_KEY + userId, "expires_at");

        if (StringUtils.hasText(cached) && StringUtils.hasText(expiresAtStr)) {
            LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
            if (LocalDateTime.now().isBefore(expiresAt.minusMinutes(5))) {
                return cached;
            }
        }

        // 2) 만료 → DB의 refresh token으로 갱신
        return refreshAndStore(userId);
    }

    @SuppressWarnings("unchecked")
    private String refreshAndStore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SmartflowException(ErrorCode.AUTH_001));

        String refreshToken = user.getGoogleRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            throw new SmartflowException(ErrorCode.MCP_001);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        try {
            Map<String, Object> response = webClient.post()
                    .uri(tokenUri)
                    .body(BodyInserters.fromFormData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || !response.containsKey("access_token")) {
                throw new SmartflowException(ErrorCode.MCP_001);
            }

            String newAccessToken = (String) response.get("access_token");
            long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

            // Redis 갱신
            redisTemplate.opsForHash().put(GOOGLE_TOKEN_KEY + userId, "access_token", newAccessToken);
            redisTemplate.opsForHash().put(GOOGLE_TOKEN_KEY + userId, "expires_at", expiresAt.toString());
            redisTemplate.expire(GOOGLE_TOKEN_KEY + userId, Duration.ofSeconds(expiresIn));

            // DB 갱신 (암호화 저장)
            user.setGoogleAccessToken(newAccessToken);
            user.setGoogleTokenExpiresAt(expiresAt);
            userRepository.save(user);

            return newAccessToken;
        } catch (SmartflowException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token refresh failed for user {}", userId, e);
            throw new SmartflowException(ErrorCode.MCP_001);
        }
    }
}
