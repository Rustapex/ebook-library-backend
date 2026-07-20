package com.acorn.abc.notification.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.notification.dto.NotificationItemResponse;
import com.acorn.abc.notification.dto.NotificationReadResponse;
import com.acorn.abc.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "본인 알림 목록을 조회합니다. 알림 본문은 저장/반환하지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationItemResponse>>> getMyNotifications(
            @RequestParam(required = false) Boolean readYn,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PageResponse<NotificationItemResponse> response =
                notificationService.getMyNotifications(userDetails.getMemberId(), readYn, notificationType, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "알림 읽음 처리", description = "본인 알림을 읽음 처리합니다. 이미 읽은 알림을 다시 호출해도 200을 반환합니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NotificationReadResponse response = notificationService.markAsRead(userDetails.getMemberId(), notificationId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
