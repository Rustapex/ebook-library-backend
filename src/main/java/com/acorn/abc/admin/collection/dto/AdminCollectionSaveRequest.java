package com.acorn.abc.admin.collection.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// 생성(API-ADMIN-COLLECTION-005 POST)과 수정(API-ADMIN-COLLECTION-002 PUT)이 동일한 필드 계약을
// 사용한다(구현계획 §6.2). SERIES/EVENT 교차 검증(할인율·기간 null 제약, 유형 변경 제한)은
// DTO만으로 표현하기 어려워 AdminCollectionService에서 수행한다(D-10 — 신규 ErrorCode 없이
// 기존 VALIDATION_ERROR만 사용).
public record AdminCollectionSaveRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
        String name,

        @NotBlank(message = "컬렉션 유형은 필수입니다.")
        @Pattern(regexp = "^(SERIES|EVENT)$", message = "컬렉션 유형은 SERIES 또는 EVENT여야 합니다.")
        String collectionType,

        @DecimalMin(value = "0", message = "할인율은 0 이상이어야 합니다.")
        @DecimalMax(value = "100", message = "할인율은 100 이하여야 합니다.")
        BigDecimal discountRate,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        @NotBlank(message = "상태는 필수입니다.")
        @Pattern(regexp = "^(ACTIVE|HIDDEN|ENDED)$", message = "상태는 ACTIVE, HIDDEN, ENDED 중 하나여야 합니다.")
        String status
) {
}
