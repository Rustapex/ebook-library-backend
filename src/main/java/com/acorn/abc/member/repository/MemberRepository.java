package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByNameAndEmail(String name, String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

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
