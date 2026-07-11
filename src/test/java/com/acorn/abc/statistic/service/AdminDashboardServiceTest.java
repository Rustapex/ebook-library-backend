package com.acorn.abc.statistic.service;

import com.acorn.abc.member.domain.Member;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.statistic.dto.AdminDashboardResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AdminStatisticService adminStatisticService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getDashboard_통계와_최근_결제_5건을_함께_반환한다() {
        AdminStatisticResponse statistics = new AdminStatisticResponse(
                "TOTAL", "ALL", LocalDate.of(2000, 1, 1), LocalDate.now(),
                5, 0, 1, 10, 7, 3, 4, 19800L, 0, 0, LocalDateTime.now());
        given(adminStatisticService.getTotalStatisticForDashboard()).willReturn(statistics);

        Member member = mock(Member.class);
        given(member.getName()).willReturn("홍길동");
        Payment payment = mock(Payment.class);
        given(payment.getPaymentId()).willReturn(1L);
        given(payment.getMember()).willReturn(member);
        given(payment.getAmount()).willReturn(9900);
        given(payment.getPaymentStatus()).willReturn("PAID");
        given(payment.getPaidAt()).willReturn(LocalDateTime.now());
        given(paymentRepository.findRecentPayments(any())).willReturn(List.of(payment));

        AdminDashboardResponse response = adminDashboardService.getDashboard();

        assertThat(response.statistics().periodType()).isEqualTo("TOTAL");
        assertThat(response.recentPayments()).hasSize(1);
        assertThat(response.recentPayments().get(0).memberName()).isEqualTo("홍길동");
        assertThat(response.recentPayments().get(0).amount()).isEqualTo(9900);
    }
}
