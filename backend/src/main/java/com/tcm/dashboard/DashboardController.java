package com.tcm.dashboard;

import com.tcm.dashboard.dto.AdminDashboardResponse;
import com.tcm.dashboard.dto.TrainerDashboardResponse;
import com.tcm.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only aggregation across every domain, per docs/tasks/TCM-29. A
 * student has no dashboard endpoint: their home page is their own summary,
 * which TCM-13 already serves.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse adminSummary() {
        return dashboardService.adminSummary();
    }

    /** A trainer's own figures - there is no id to pass, so no one else's. */
    @GetMapping("/trainer-summary")
    @PreAuthorize("hasRole('TRAINER')")
    public TrainerDashboardResponse trainerSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.trainerSummary(principal.getId());
    }
}
