package com.acorn.abc.admin.book.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// 등록(API-ADMIN-BOOK-002 POST)과 수정(API-ADMIN-BOOK-003 PUT)이 동일한 필드 계약을 사용한다
// (관리자 도서 API 설계문서 §2.2/§2.3). status는 요청에 없다 — 등록은 항상 INACTIVE, 상태 변경은
// API-ADMIN-BOOK-004 전용. keywords[]/file/pages[]/detailContent도 이 API의 보호 대상이라 필드 자체가 없다.
public record AdminBookSaveRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
        String title,

        @Pattern(regexp = "^\\d{13}$", message = "ISBN은 숫자 13자리여야 합니다.")
        String isbn,

        @NotBlank(message = "출판사명은 필수입니다.")
        @Size(max = 100, message = "출판사명은 100자 이하로 입력해주세요.")
        String publisherName,

        @NotEmpty(message = "저자는 1명 이상 입력해주세요.")
        @Size(max = 10, message = "저자는 최대 10명까지 입력할 수 있습니다.")
        List<@NotBlank(message = "저자명은 공백일 수 없습니다.")
             @Size(max = 100, message = "저자명은 100자 이하로 입력해주세요.") String> authors,

        @NotNull(message = "categoryIds는 필수입니다.")
        @Size(max = 20, message = "카테고리는 최대 20개까지 선택할 수 있습니다.")
        List<@NotNull(message = "categoryId는 null일 수 없습니다.") Long> categoryIds,

        @NotBlank(message = "대여 유형은 필수입니다.")
        @Pattern(regexp = "^(FREE|PAID)$", message = "대여 유형은 FREE 또는 PAID여야 합니다.")
        String rentalType,

        @Min(value = 0, message = "대여 가격은 0 이상이어야 합니다.")
        int rentalPrice,

        @Min(value = 1, message = "기본 대여일은 1 이상이어야 합니다.")
        int defaultRentalDays,

        @Size(max = 500, message = "표지 이미지 URL은 500자 이하로 입력해주세요.")
        String coverImageUrl,

        @Valid
        Detail detail
) {
    // description/tableOfContents/publisherReview만 허용한다. detailContent(실 열람 원문)는 필드 자체가 없다.
    public record Detail(
            @Size(max = 5000, message = "책 소개는 5000자 이하로 입력해주세요.")
            String description,

            @Size(max = 5000, message = "목차는 5000자 이하로 입력해주세요.")
            String tableOfContents,

            @Size(max = 5000, message = "출판사 리뷰는 5000자 이하로 입력해주세요.")
            String publisherReview
    ) {
    }
}
