package com.smartflow.controller;

import com.smartflow.domain.User;
import com.smartflow.dto.response.ApiResponse;
import com.smartflow.dto.response.AuthResponse;
import com.smartflow.exception.ErrorCode;
import com.smartflow.exception.SmartflowException;
import com.smartflow.repository.UserRepository;
import com.smartflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new SmartflowException(ErrorCode.AUTH_001));
        return ResponseEntity.ok(ApiResponse.ok(
                AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build()
        ));
    }
}
