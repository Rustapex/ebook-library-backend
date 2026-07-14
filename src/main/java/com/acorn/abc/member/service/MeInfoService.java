package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberGradeSummaryResponse;
import com.acorn.abc.member.repository.MemberGradeRepository;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.notification.repository.NotificationRepository;
import com.acorn.abc.payment.repository.MemberCouponRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeInfoService {

    private static final String COUPON_STATUS_ISSUED = "ISSUED";
    private static final String PAYMENT_STATUS_PAID = "PAID";

    private final MemberRepository memberRepository;
    private final MemberGradeRepository memberGradeRepository;
    private final PaymentRepository paymentRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final FavoriteBookRepository favoriteBookRepository;
    private final NotificationRepository notificationRepository;

    public MeInfoResponse getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        MemberGradeSummaryResponse grade = buildGradeSummary(member);
        long couponCount = memberCouponRepository.countUsableByMember(memberId, COUPON_STATUS_ISSUED, LocalDateTime.now());
        long rentalCount = bookRentalRepository.countByMember_MemberId(memberId);
        long completedBookCount = readingProgressRepository.countCompletedByMemberAndPeriod(memberId, null, null);
        long favoriteCount = favoriteBookRepository.countByMember_MemberId(memberId);
        long unreadNotificationCount = notificationRepository.countByMember_MemberIdAndReadYnFalse(memberId);

        return MeInfoResponse.of(member, grade, couponCount, rentalCount, completedBookCount, favoriteCount, unreadNotificationCount);
    }

    private MemberGradeSummaryResponse buildGradeSummary(Member member) {
        MemberGrade currentGrade = member.getGrade();
        LocalDateTime paymentStartAt = member.getGradePaymentStartedAt() != null
                ? member.getGradePaymentStartedAt()
                : member.getCreatedAt();
        long currentPaymentAmount = paymentRepository.sumAmountByMemberAndStatusSince(
                member.getMemberId(), PAYMENT_STATUS_PAID, paymentStartAt);

        Optional<MemberGrade> nextGrade = memberGradeRepository.findByGradeLevel(currentGrade.getGradeLevel() + 1);
        if (nextGrade.isEmpty()) {
            return new MemberGradeSummaryResponse(
                    currentGrade.getGradeId(), currentGrade.getGradeName(), currentGrade.getGradeLevel(),
                    null, null, currentPaymentAmount, null, 100.0);
        }

        MemberGrade next = nextGrade.get();
        long targetPaymentAmount = next.getMinPaymentAmount();
        double progressRate = calculateProgressRate(currentPaymentAmount, targetPaymentAmount);

        return new MemberGradeSummaryResponse(
                currentGrade.getGradeId(), currentGrade.getGradeName(), currentGrade.getGradeLevel(),
                next.getGradeId(), next.getGradeName(), currentPaymentAmount, targetPaymentAmount, progressRate);
    }

    private double calculateProgressRate(long currentPaymentAmount, long targetPaymentAmount) {
        if (targetPaymentAmount <= 0) {
            return 100.0;
        }
        BigDecimal raw = BigDecimal.valueOf(currentPaymentAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(targetPaymentAmount), 2, RoundingMode.HALF_UP);
        return raw.min(BigDecimal.valueOf(100)).doubleValue();
    }
}
