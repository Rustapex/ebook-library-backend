package com.acorn.abc.member.service;

import com.acorn.abc.challenge.service.ChallengeService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Attendance;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.dto.AttendanceCheckResponse;
import com.acorn.abc.member.dto.AttendanceMonthResponse;
import com.acorn.abc.member.dto.IssuedCouponResponse;
import com.acorn.abc.member.repository.AttendanceRepository;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final ChallengeService challengeService;

    public AttendanceCheckResponse checkIn(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDate today = LocalDate.now();
        if (attendanceRepository.existsByMember_MemberIdAndAttendanceDate(memberId, today)) {
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_EXISTS);
        }
        attendanceRepository.save(Attendance.create(member, today));
        challengeService.updateProgressOnAttendance(memberId);
        int consecutiveDays = calculateConsecutiveDays(memberId);

        // TODO(2026-07-06 계획 4-2 이후 미결 항목): 7일/30일 연속 출석 쿠폰 지급 보류.
        // coupon 테이블엔 "이 쿠폰이 7일/30일 출석 보상용"임을 식별할 컬럼이 없어,
        // 어떤 coupon_id를 지급할지 결정하는 방법(couponType 컨벤션 등)을 확정한 뒤 연동 예정.
        List<IssuedCouponResponse> issuedCoupons = List.of();

        return new AttendanceCheckResponse(today, consecutiveDays, issuedCoupons);
    }

    @Transactional(readOnly = true)
    public AttendanceMonthResponse getMonthlyAttendance(Long memberId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<LocalDate> attendanceDates = attendanceRepository
                .findByMember_MemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(memberId, start, end)
                .stream()
                .map(Attendance::getAttendanceDate)
                .toList();
        boolean todayCheckedYn = attendanceRepository.existsByMember_MemberIdAndAttendanceDate(memberId, LocalDate.now());
        int consecutiveDays = calculateConsecutiveDays(memberId);
        return new AttendanceMonthResponse(attendanceDates, todayCheckedYn, consecutiveDays);
    }

    private int calculateConsecutiveDays(Long memberId) {
        List<Attendance> recent = attendanceRepository.findByMember_MemberIdOrderByAttendanceDateDesc(memberId);
        if (recent.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate mostRecent = recent.get(0).getAttendanceDate();
        if (mostRecent.isBefore(today.minusDays(1))) {
            return 0;
        }
        int streak = 0;
        LocalDate expected = mostRecent;
        for (Attendance attendance : recent) {
            if (attendance.getAttendanceDate().isEqual(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (attendance.getAttendanceDate().isBefore(expected)) {
                break;
            }
        }
        return streak;
    }
}
