package com.princekumar.itams.stats.dto;

import java.util.List;

/**
 * Everything the management dashboard needs in one call — cheaper than
 * fanning out 8 requests from the browser.
 */
public record DashboardStatsResponse(
    // top-line KPIs
    long totalAssets,
    long availableAssets,
    long assignedAssets,
    long assetsUnderMaintenance,
    long retiredAssets,
    long totalEmployees,
    long openTickets,
    long openAssignments,
    long licensesExpiringSoon,  // within 60 days

    // breakdowns
    List<CountByLabel> assetsByStatus,
    List<CountByLabel> assetsByCategory,
    List<CountByLabel> assetsByDepartment,   // via active assignments
    List<CountByLabel> ticketsByPriority,
    List<CountByLabel> ticketsByStatus
) {}
