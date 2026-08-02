package com.acorn.abc.notification.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.notification.dto.NoticeDetailResponse;
import com.acorn.abc.notification.dto.NoticeItemResponse;
import com.acorn.abc.notification.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notice", description = "공지 API")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록 조회", description = "ACTIVE 상태 공지만 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeItemResponse>>> getNotices(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getNotices(page, size)));
    }

    @Operation(summary = "공지 상세 조회", description = "ACTIVE 상태 공지만 조회 가능하며 이전/다음 공지를 함께 반환합니다.")
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNoticeDetail(@PathVariable Long noticeId) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getNoticeDetail(noticeId)));
    }
}
