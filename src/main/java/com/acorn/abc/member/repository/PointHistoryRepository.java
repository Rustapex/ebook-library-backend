package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByMember_MemberId(Long memberId, Pageable pageable);

    Page<PointHistory> findByMember_MemberIdAndPointType(Long memberId, String pointType, Pageable pageable);

    Optional<PointHistory> findFirstByPayment_PaymentId(Long paymentId);
}
