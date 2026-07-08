package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.dto.PointHistoryResponse;
import com.acorn.abc.member.dto.PointSummaryResponse;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointHistory grantOrAdjustPoint(Long memberId, int amount, String pointType, String description) {
        int updatedRows = memberRepository.applyPointChangeIfSufficient(memberId, amount);
        if (updatedRows == 0) {
            if (!memberRepository.existsById(memberId)) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        PointHistory pointHistory = PointHistory.create(member, null, pointType, amount, description);
        return pointHistoryRepository.save(pointHistory);
    }

    public AdminPointAdjustResponse adjustPointByAdmin(Long memberId, int amount, String description) {
        PointHistory history = grantOrAdjustPoint(memberId, amount, "ADMIN_ADJUST", description);
        return new AdminPointAdjustResponse(
                history.getPointHistoryId(), history.getPointAmount(), history.getPointType(),
                history.getMember().getPointBalance(), history.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PointSummaryResponse getPointHistory(Long memberId, String pointType, int page, int size) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PointHistory> result = (pointType == null || pointType.isBlank())
                ? pointHistoryRepository.findByMember_MemberId(memberId, pageable)
                : pointHistoryRepository.findByMember_MemberIdAndPointType(memberId, pointType, pageable);

        List<PointHistoryResponse> content = result.getContent().stream()
                .map(ph -> new PointHistoryResponse(
                        ph.getPointHistoryId(), ph.getPointType(), ph.getPointAmount(),
                        ph.getDescription(), ph.getCreatedAt()))
                .toList();

        PageResponse<PointHistoryResponse> pageResponse =
                PageResponse.of(content, page, size, result.getTotalElements());
        return new PointSummaryResponse(member.getPointBalance(), pageResponse);
    }
}
