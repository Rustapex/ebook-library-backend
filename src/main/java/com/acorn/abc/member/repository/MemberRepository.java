package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByNameAndEmail(String name, String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    long countByStatus(String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Member m SET m.pointBalance = m.pointBalance + :amount "
            + "WHERE m.memberId = :memberId AND m.pointBalance + :amount >= 0")
    int applyPointChangeIfSufficient(@Param("memberId") Long memberId, @Param("amount") int amount);
}
