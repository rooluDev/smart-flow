package com.smartflow.service.auth;

import com.smartflow.domain.User;
import com.smartflow.dto.response.AuthResponse;
import com.smartflow.exception.ErrorCode;
import com.smartflow.exception.SmartflowException;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "smartflow:refresh:";
    private static final String GOOGLE_TOKEN_KEY_PREFIX = "smartflow:google_token:";
    private static final long REFRESH_TOKEN_TTL_DAYS = 7L;

    private final UserRepository userRepository;
    private final GoogleOAuthClient googleOAuthClient;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public AuthResponse loginWithGoogle(String code, String redirectUri) {
        GoogleOAuthClient.TokenResponse tokens = googleOAuthClient.exchangeCodeForTokens(code, redirectUri);
        GoogleOAuthClient.UserInfo userInfo = googleOAuthClient.getUserInfo(tokens.getAccessToken());

        User user = userRepository.findByGoogleId(userInfo.getGoogleId())
                .map(existing -> updateUser(existing, userInfo, tokens))
                .orElseGet(() -> createUser(userInfo, tokens));

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        storeRefreshToken(user.getId(), refreshToken);
        storeGoogleToken(user.getId(), tokens.getAccessToken());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .build();
    }

    public AuthResponse.TokenRefreshDto refreshAccessToken(String refreshToken) {
        jwtService.validateToken(refreshToken);
        Long userId = jwtService.extractUserId(refreshToken);

        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (stored == null) throw new SmartflowException(ErrorCode.AUTH_006);
        if (!stored.equals(refreshToken)) throw new SmartflowException(ErrorCode.AUTH_007);

        String newAccessToken = jwtService.generateAccessToken(userId);
        return AuthResponse.TokenRefreshDto.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        redisTemplate.delete(GOOGLE_TOKEN_KEY_PREFIX + userId);
        redisTemplate.delete("smartflow:dashboard:" + userId);
    }

    private User createUser(GoogleOAuthClient.UserInfo info, GoogleOAuthClient.TokenResponse tokens) {
        User user = User.builder()
                .googleId(info.getGoogleId())
                .email(info.getEmail())
                .name(info.getName())
                .profileImageUrl(info.getProfileImageUrl())
                .googleAccessToken(tokens.getAccessToken())
                .googleRefreshToken(tokens.getRefreshToken())
                .googleTokenExpiresAt(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()))
                .build();
        return userRepository.save(user);
    }

    private User updateUser(User user, GoogleOAuthClient.UserInfo info, GoogleOAuthClient.TokenResponse tokens) {
        user.setName(info.getName());
        user.setProfileImageUrl(info.getProfileImageUrl());
        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        user.setGoogleTokenExpiresAt(LocalDateTime.now().plusSeconds(tokens.getExpiresIn()));
        return userRepository.save(user);
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                refreshToken,
                REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS
        );
    }

    private void storeGoogleToken(Long userId, String accessToken) {
        redisTemplate.opsForHash().put(GOOGLE_TOKEN_KEY_PREFIX + userId, "access_token", accessToken);
        redisTemplate.opsForHash().put(GOOGLE_TOKEN_KEY_PREFIX + userId, "expires_at",
                LocalDateTime.now().plusHours(1).toString());
    }
}
