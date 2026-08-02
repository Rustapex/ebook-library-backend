package com.acorn.abc.admin.request.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

// candidate_status가 REQUESTED/IN_REVIEW인 동안은 approvedBookId/rejectReason이 null이므로
// 응답 JSON에서 키 자체를 생략한다(NON_NULL). APPROVED/REJECTED일 때만 해당 키가 값과 함께 내려간다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminBookRequestCandidateResponse(
        Long candidateId,
        String title,
        String author,
        String publisher,
        String status,
        int requestCount,
        LocalDateTime firstRequestedAt,
        LocalDateTime latestRequestedAt,
        List<AdminBookRequestApplicantResponse> applicants,
        String rejectReason,
        Long approvedBookId
) {
}
