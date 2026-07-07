package com.fintrack.web.controller;

import com.fintrack.security.UserPrincipal;
import com.fintrack.service.dashboard.DashboardService;
import com.fintrack.web.dto.response.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dashboardService.getDashboard(principal.getId()));
    }
}