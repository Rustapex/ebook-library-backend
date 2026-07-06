package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByMember_MemberIdAndAttendanceDate(Long memberId, LocalDate attendanceDate);

    List<Attendance> findByMember_MemberIdOrderByAttendanceDateDesc(Long memberId);

    List<Attendance> findByMember_MemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long memberId, LocalDate startDate, LocalDate endDate);
}
