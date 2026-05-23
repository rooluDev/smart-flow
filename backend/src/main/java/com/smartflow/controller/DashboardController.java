package com.smartflow.controller;

import com.smartflow.dto.response.ApiResponse;
import com.smartflow.dto.response.DashboardResponse;
import com.smartflow.security.UserPrincipal;
import com.smartflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        DashboardResponse response = dashboardService.getDashboard(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> invalidateCache(
            @AuthenticationPrincipal UserPrincipal principal) {
        dashboardService.invalidateCache(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
