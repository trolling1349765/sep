package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.DashboardResponse;
import fpt.capstone.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_MONITOR_VIEW')")
    public ResponseEntity<APIResponse<DashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "5") int recentSize) {
        return ResponseEntity.ok(APIResponse.success(dashboardService.getDashboard(recentSize)));
    }
}
