package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.repository.MemberGradeRepository;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.notification.repository.NotificationRepository;
import com.acorn.abc.payment.repository.MemberCouponRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeInfoServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberGradeRepository memberGradeRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private BookRentalRepository bookRentalRepository;

    @Mock
    private ReadingProgressRepository readingProgressRepository;

    @Mock
    private FavoriteBookRepository favoriteBookRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private MeInfoService meInfoService;

    @Test
    void getMe_기본정보와_모든_요약_카운트를_응답에_조합한다() {
        LocalDateTime paymentStartAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Member member = mockMember(mockCurrentGrade(1L, "씨앗", 1), 3000, paymentStartAt, paymentStartAt.minusYears(1));
        MemberGrade nextGrade = mockNextGrade(2L, "새싹", 20000);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberGradeRepository.findByGradeLevel(2)).willReturn(Optional.of(nextGrade));
        given(paymentRepository.sumAmountByMemberAndStatusSince(MEMBER_ID, "PAID", paymentStartAt)).willReturn(10000L);
        given(memberCouponRepository.countUsableByMember(eq(MEMBER_ID), eq("ISSUED"), any())).willReturn(2L);
        given(bookRentalRepository.countByMember_MemberId(MEMBER_ID)).willReturn(5L);
        given(readingProgressRepository.countCompletedByMemberAndPeriod(MEMBER_ID, null, null)).willReturn(3L);
        given(favoriteBookRepository.countByMember_MemberId(MEMBER_ID)).willReturn(4L);
        given(notificationRepository.countByMember_MemberIdAndReadYnFalse(MEMBER_ID)).willReturn(1L);

        MeInfoResponse response = meInfoService.getMe(MEMBER_ID);

        assertThat(response.pointBalance()).isEqualTo(3000);
        assertThat(response.couponCount()).isEqualTo(2L);
        assertThat(response.rentalCount()).isEqualTo(5L);
        assertThat(response.completedBookCount()).isEqualTo(3L);
        assertThat(response.favoriteCount()).isEqualTo(4L);
        assertThat(response.unreadNotificationCount()).isEqualTo(1L);
        assertThat(response.grade().gradeId()).isEqualTo(1L);
        assertThat(response.grade().gradeName()).isEqualTo("씨앗");
        assertThat(response.grade().nextGradeId()).isEqualTo(2L);
        assertThat(response.grade().nextGradeName()).isEqualTo("새싹");
        assertThat(response.grade().currentPaymentAmount()).isEqualTo(10000L);
        assertThat(response.grade().targetPaymentAmount()).isEqualTo(20000L);
        assertThat(response.grade().progressRate()).isEqualTo(50.0);
    }

    @Test
    void getMe_등급산정시작일이_null이면_가입일을_결제집계_시작일로_사용한다() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 6, 1, 0, 0);
        Member member = mockMember(mockCurrentGrade(1L, "씨앗", 1), 0, null, createdAt);
        MemberGrade nextGrade = mockNextGrade(2L, "새싹", 20000);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberGradeRepository.findByGradeLevel(2)).willReturn(Optional.of(nextGrade));
        given(paymentRepository.sumAmountByMemberAndStatusSince(MEMBER_ID, "PAID", createdAt)).willReturn(0L);
        stubZeroCounts();

        meInfoService.getMe(MEMBER_ID);

        verify(paymentRepository).sumAmountByMemberAndStatusSince(MEMBER_ID, "PAID", createdAt);
    }

    @Test
    void getMe_달성률이_100을_넘으면_100으로_제한한다() {
        Member member = mockMember(mockCurrentGrade(1L, "씨앗", 1), 0, LocalDateTime.now(), null);
        MemberGrade nextGrade = mockNextGrade(2L, "새싹", 20000);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberGradeRepository.findByGradeLevel(2)).willReturn(Optional.of(nextGrade));
        given(paymentRepository.sumAmountByMemberAndStatusSince(eq(MEMBER_ID), eq("PAID"), any())).willReturn(30000L);
        stubZeroCounts();

        MeInfoResponse response = meInfoService.getMe(MEMBER_ID);

        assertThat(response.grade().progressRate()).isEqualTo(100.0);
    }

    @Test
    void getMe_최고등급이면_다음등급정보는_null이고_달성률은_100이다() {
        Member member = mockMember(mockCurrentGrade(4L, "도토리", 4), 0, LocalDateTime.now(), null);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberGradeRepository.findByGradeLevel(5)).willReturn(Optional.empty());
        given(paymentRepository.sumAmountByMemberAndStatusSince(eq(MEMBER_ID), eq("PAID"), any())).willReturn(999999L);
        stubZeroCounts();

        MeInfoResponse response = meInfoService.getMe(MEMBER_ID);

        assertThat(response.grade().nextGradeId()).isNull();
        assertThat(response.grade().nextGradeName()).isNull();
        assertThat(response.grade().targetPaymentAmount()).isNull();
        assertThat(response.grade().progressRate()).isEqualTo(100.0);
    }

    @Test
    void getMe_회원이_없으면_MEMBER_NOT_FOUND를_던진다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> meInfoService.getMe(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(memberCouponRepository, never()).countUsableByMember(anyLong(), any(), any());
    }

    private void stubZeroCounts() {
        given(memberCouponRepository.countUsableByMember(eq(MEMBER_ID), eq("ISSUED"), any())).willReturn(0L);
        given(bookRentalRepository.countByMember_MemberId(MEMBER_ID)).willReturn(0L);
        given(readingProgressRepository.countCompletedByMemberAndPeriod(MEMBER_ID, null, null)).willReturn(0L);
        given(favoriteBookRepository.countByMember_MemberId(MEMBER_ID)).willReturn(0L);
        given(notificationRepository.countByMember_MemberIdAndReadYnFalse(MEMBER_ID)).willReturn(0L);
    }

    private Member mockMember(MemberGrade grade, int pointBalance, LocalDateTime gradePaymentStartedAt, LocalDateTime createdAt) {
        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(MEMBER_ID);
        given(member.getLoginId()).willReturn("member0714a");
        given(member.getName()).willReturn("회원A");
        given(member.getEmail()).willReturn("member0714a@example.com");
        given(member.getPhone()).willReturn("010-0714-0001");
        given(member.getBirthDate()).willReturn(LocalDate.of(1995, 7, 14));
        given(member.getGender()).willReturn("MALE");
        given(member.getRole()).willReturn("USER");
        given(member.getStatus()).willReturn("JOINED");
        given(member.getGrade()).willReturn(grade);
        given(member.getPointBalance()).willReturn(pointBalance);
        given(member.getGradePaymentStartedAt()).willReturn(gradePaymentStartedAt);
        if (gradePaymentStartedAt == null) {
            given(member.getCreatedAt()).willReturn(createdAt);
        }
        return member;
    }

    private MemberGrade mockCurrentGrade(Long gradeId, String gradeName, int gradeLevel) {
        MemberGrade grade = mock(MemberGrade.class);
        given(grade.getGradeId()).willReturn(gradeId);
        given(grade.getGradeName()).willReturn(gradeName);
        given(grade.getGradeLevel()).willReturn(gradeLevel);
        return grade;
    }

    private MemberGrade mockNextGrade(Long gradeId, String gradeName, int minPaymentAmount) {
        MemberGrade grade = mock(MemberGrade.class);
        given(grade.getGradeId()).willReturn(gradeId);
        given(grade.getGradeName()).willReturn(gradeName);
        given(grade.getMinPaymentAmount()).willReturn(minPaymentAmount);
        return grade;
    }
}
