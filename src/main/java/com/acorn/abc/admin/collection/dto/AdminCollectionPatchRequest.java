package com.acorn.abc.admin.collection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

// status와 removeBookIds 중 하나 이상 필수(D-05) — 둘 다 필드 자체가 optional이라 DTO 단독으로는
// 표현할 수 없어 AdminCollectionService에서 "둘 다 없으면 VALIDATION_ERROR"를 검증한다.
// removeBookIds 원소는 List<@NotNull @Positive Long>로 선언해 JSON [null], [0], [-1]을 요청
// 검증 단계에서 400으로 차단한다(PR50 코드리뷰 MEDIUM). 리스트 자체가 null인 것(필드 생략)은 허용한다.
public record AdminCollectionPatchRequest(

        @Pattern(regexp = "^(ACTIVE|HIDDEN|ENDED)$", message = "상태는 ACTIVE, HIDDEN, ENDED 중 하나여야 합니다.")
        String status,

        List<@NotNull(message = "제거할 도서 ID는 null일 수 없습니다.")
             @Positive(message = "제거할 도서 ID는 양수여야 합니다.") Long> removeBookIds
) {
}
