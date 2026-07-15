package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.MemberGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberGradeRepository extends JpaRepository<MemberGrade, Long> {

    Optional<MemberGrade> findByGradeLevel(int gradeLevel);
}
