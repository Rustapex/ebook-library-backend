package com.acorn.abc.statistic.service;

import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.statistic.dto.AdminDashboardResponse;
import com.acorn.abc.statistic.dto.AdminRecentPaymentResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_PAYMENT_LIMIT = 5;

    private final AdminStatisticService adminStatisticService;
    private final PaymentRepository paymentRepository;

    public AdminDashboardResponse getDashboard() {
        AdminStatisticResponse statistics = adminStatisticService.getTotalStatisticForDashboard();

        Pageable pageable = PageRequest.of(0, RECENT_PAYMENT_LIMIT);
        List<AdminRecentPaymentResponse> recentPayments = paymentRepository.findRecentPayments(pageable).stream()
                .map(AdminRecentPaymentResponse::from)
                .toList();

        return new AdminDashboardResponse(statistics, recentPayments);
    }
}
