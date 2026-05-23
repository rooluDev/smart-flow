package com.smartflow.controller;

import com.smartflow.dto.request.ConversationTitleUpdateRequest;
import com.smartflow.dto.response.ApiResponse;
import com.smartflow.dto.response.ConversationResponse;
import com.smartflow.security.UserPrincipal;
import com.smartflow.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<ApiResponse<ConversationResponse.ListResponse>> getList(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                conversationService.getList(principal.getUserId(), page, size)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse.CreateResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(ApiResponse.ok(
                conversationService.create(principal.getUserId())
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                conversationService.getOne(id, principal.getUserId())
        ));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<ApiResponse<ConversationResponse.TitleUpdateResponse>> updateTitle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ConversationTitleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                conversationService.updateTitle(id, principal.getUserId(), request.getTitle())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        conversationService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
