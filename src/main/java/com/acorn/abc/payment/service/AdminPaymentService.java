package com.acorn.abc.payment.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.payment.domain.CouponUsage;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.dto.AdminPaymentSummaryResponse;
import com.acorn.abc.payment.repository.CouponUsageRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자 결제 목록 조회(API-ADMIN-PAYMENT-001) 서비스. PAID만 조회하며 취소/환불/복구는 다루지 않는다(구현계획 §2.3).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PageResponse<AdminPaymentSummaryResponse> getAdminPayments(String q, LocalDate from, LocalDate to,
                                                                         int page, int size) {
        validatePaging(page, size);

        String likeQ = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();

        Page<Payment> result = paymentRepository.searchForAdmin(likeQ, fromDateTime, toDateTime, PageRequest.of(page, size));

        List<Long> paymentIds = result.getContent().stream().map(Payment::getPaymentId).toList();

        Map<Long, Integer> couponDiscountByPaymentId = paymentIds.isEmpty() ? Map.of()
                : couponUsageRepository.findByPayment_PaymentIdIn(paymentIds).stream()
                    .collect(Collectors.toMap(u -> u.getPayment().getPaymentId(), CouponUsage::getDiscountAmount, (a, b) -> a));

        Map<Long, Integer> pointUsedByPaymentId = paymentIds.isEmpty() ? Map.of()
                : pointHistoryRepository.findByPayment_PaymentIdIn(paymentIds).stream()
                    .filter(h -> "PAYMENT_USE".equals(h.getPointType()))
                    .collect(Collectors.toMap(h -> h.getPayment().getPaymentId(), h -> -h.getPointAmount(), (a, b) -> a));

        List<AdminPaymentSummaryResponse> content = result.getContent().stream()
                .map(p -> toSummary(p, couponDiscountByPaymentId.getOrDefault(p.getPaymentId(), 0),
                        pointUsedByPaymentId.getOrDefault(p.getPaymentId(), 0)))
                .toList();

        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    private AdminPaymentSummaryResponse toSummary(Payment payment, int couponDiscountAmount, int pointUsedAmount) {
        return new AdminPaymentSummaryResponse(
                payment.getPaymentId(),
                payment.getMember().getMemberId(),
                payment.getMember().getName(),
                payment.getRental().getRentalId(),
                payment.getRental().getBook().getBookId(),
                payment.getRental().getBook().getTitle(),
                payment.getPaymentMethod(),
                payment.getOriginalAmount(),
                couponDiscountAmount,
                pointUsedAmount,
                payment.getAmount(),
                payment.getPaidAt()
        );
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
