package com.acorn.abc.admin.collection.dto;

import jakarta.validation.constraints.Pattern;

import java.util.List;

// status와 removeBookIds 중 하나 이상 필수(D-05) — 둘 다 필드 자체가 optional이라 DTO 단독으로는
// 표현할 수 없어 AdminCollectionService에서 "둘 다 없으면 VALIDATION_ERROR"를 검증한다.
public record AdminCollectionPatchRequest(

        @Pattern(regexp = "^(ACTIVE|HIDDEN|ENDED)$", message = "상태는 ACTIVE, HIDDEN, ENDED 중 하나여야 합니다.")
        String status,

        List<Long> removeBookIds
) {
}
