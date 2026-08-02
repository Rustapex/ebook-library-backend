package com.acorn.abc.member.repository;

import com.acorn.abc.member.domain.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByMember_MemberId(Long memberId, Pageable pageable);

    // 관리자 결제 목록 조회(API-ADMIN-PAYMENT-001, 2026-07-22 구현계획 §2.3) — payment id 목록으로
    // 포인트 차감 이력을 한 번에 bulk 조회한다(N+1 방지).
    List<PointHistory> findByPayment_PaymentIdIn(List<Long> paymentIds);

    Page<PointHistory> findByMember_MemberIdAndPointType(Long memberId, String pointType, Pageable pageable);

    Optional<PointHistory> findFirstByPayment_PaymentId(Long paymentId);

    // 관리자 포인트 조정(API-ADMIN-MEMBER-004) 멱등 키 조회 — (member_id, request_id) UNIQUE와 짝을 이룬다.
    // replay 응답이 existing.getMember().getPointBalance()를 읽으므로, 이 조회 자체가 자체 트랜잭션으로
    // 끝나는 AdminPointAdjustService(비-@Transactional)에서도 LazyInitializationException 없이 접근할 수
    // 있도록 member를 함께 fetch한다.
    @Query("SELECT p FROM PointHistory p JOIN FETCH p.member WHERE p.member.memberId = :memberId AND p.requestId = :requestId")
    Optional<PointHistory> findByMember_MemberIdAndRequestId(@Param("memberId") Long memberId, @Param("requestId") String requestId);

    // 관리자 회원 상세 POINTS 탭 — createdAt DESC, pointHistoryId DESC 고정(구현계획 §1.3).
    @Query(value = "SELECT p FROM PointHistory p WHERE p.member.memberId = :memberId " +
            "ORDER BY p.createdAt DESC, p.pointHistoryId DESC",
            countQuery = "SELECT COUNT(p) FROM PointHistory p WHERE p.member.memberId = :memberId")
    Page<PointHistory> findByMemberIdForAdmin(@Param("memberId") Long memberId, Pageable pageable);
}
