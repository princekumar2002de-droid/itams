package com.princekumar.itams.stats;

import com.princekumar.itams.stats.dto.DashboardStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics", description = "Aggregate dashboards and KPIs")
public class StatsController {

    private final StatsService service;
    public StatsController(StatsService service) { this.service = service; }

    @Operation(summary = "One-shot dashboard KPIs — everything the management dashboard needs")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard")
    public DashboardStatsResponse dashboard() {
        return service.dashboard();
    }
}
