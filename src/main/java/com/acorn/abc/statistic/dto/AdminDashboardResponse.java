package com.acorn.abc.statistic.dto;

import java.util.List;

public record AdminDashboardResponse(
        AdminStatisticResponse statistics,
        List<AdminRecentPaymentResponse> recentPayments,
        List<AdminRecentReportResponse> recentReports,
        List<AdminRecentBookRequestResponse> recentBookRequests
) {
}
