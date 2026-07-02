package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByNameAndEmail(String name, String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);
}
