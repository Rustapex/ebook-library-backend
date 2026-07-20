package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 관리자 회원 API 구현계획(2026-07-16) §1.4/§1.5/§1.6 — 상태·제재·포인트 변경의 write 경로 전용 잠금 조회.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.memberId = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);

    // 관리자 회원 목록(API-ADMIN-MEMBER-001) — q는 이름/아이디/이메일 부분일치(호출부에서 소문자+% 래핑), 나머지는 선택 필터.
    @Query("SELECT m FROM Member m WHERE " +
            "(:q IS NULL OR LOWER(m.name) LIKE :q OR LOWER(m.loginId) LIKE :q OR LOWER(m.email) LIKE :q) AND " +
            "(:status IS NULL OR m.status = :status) AND " +
            "(:role IS NULL OR m.role = :role) AND " +
            "(:gradeId IS NULL OR m.grade.gradeId = :gradeId) " +
            "ORDER BY m.createdAt DESC, m.memberId DESC")
    Page<Member> searchForAdmin(@Param("q") String q, @Param("status") String status,
                                 @Param("role") String role, @Param("gradeId") Long gradeId, Pageable pageable);

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByNameAndEmail(String name, String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndMemberIdNot(String email, Long memberId);

    long countByStatus(String status);

    // birthDateFrom(초과)/birthDateTo(이하) 둘 다 null이면 나이대 필터 없음(관리자 통계 §3.2 나이대 계산 기준)
    @Query("SELECT COUNT(m) FROM Member m WHERE " +
            "(:birthDateFrom IS NULL OR m.birthDate > :birthDateFrom) AND " +
            "(:birthDateTo IS NULL OR m.birthDate <= :birthDateTo)")
    long countByAgeBand(@Param("birthDateFrom") LocalDate birthDateFrom, @Param("birthDateTo") LocalDate birthDateTo);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = :status AND " +
            "(:birthDateFrom IS NULL OR m.birthDate > :birthDateFrom) AND " +
            "(:birthDateTo IS NULL OR m.birthDate <= :birthDateTo)")
    long countByStatusAndAgeBand(@Param("status") String status,
                                  @Param("birthDateFrom") LocalDate birthDateFrom,
                                  @Param("birthDateTo") LocalDate birthDateTo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Member m SET m.pointBalance = m.pointBalance + :amount "
            + "WHERE m.memberId = :memberId AND m.pointBalance + :amount >= 0")
    int applyPointChangeIfSufficient(@Param("memberId") Long memberId, @Param("amount") int amount);
}
