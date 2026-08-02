package com.acorn.abc.notification.service;

import com.acorn.abc.admin.notification.dto.AdminNoticeCreateResponse;
import com.acorn.abc.admin.notification.dto.AdminNoticeListCriteria;
import com.acorn.abc.admin.notification.dto.AdminNoticeResponse;
import com.acorn.abc.admin.notification.dto.AdminNoticeSaveRequest;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.notification.domain.Notice;
import com.acorn.abc.notification.repository.NoticeRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminNoticeService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int FAN_OUT_CHUNK_SIZE = 500;
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "HIDDEN");

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final AdminAuditLogWriter adminAuditLogWriter;

    @Transactional(readOnly = true)
    public PageResponse<AdminNoticeResponse> getAdminNotices(String status, Integer page, Integer size) {
        AdminNoticeListCriteria criteria = new AdminNoticeListCriteria(
                normalizeStatusFilter(status), normalizePage(page), normalizeSize(size));
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
        Page<Notice> notices = noticeRepository.searchForAdmin(criteria.status(), pageable);
        List<AdminNoticeResponse> content = notices.getContent().stream().map(AdminNoticeResponse::from).toList();
        return PageResponse.of(content, notices.getNumber(), notices.getSize(), notices.getTotalElements());
    }

    public AdminNoticeCreateResponse createNotice(Long adminMemberId, AdminNoticeSaveRequest request) {
        String status = normalizeRequiredStatus(request.status());
        validateHiddenNotifyCombo(status, request.notifyYn());

        Notice notice = Notice.create(request.title(), request.content(), status, request.notifyYn(), LocalDateTime.now());
        noticeRepository.save(notice);
        noticeRepository.flush(); // notice_id 채번 확보(감사 로그 targetId, 알림 targetId에 필요)

        int notifyCreatedCount = 0;
        if (Boolean.TRUE.equals(request.notifyYn())) {
            notifyCreatedCount = fanOutNoticeNotification(notice.getNoticeId(), notificationTitle(notice.getTitle()));
        }

        Map<String, Object> afterSummary = new LinkedHashMap<>();
        afterSummary.put("title", notice.getTitle());
        afterSummary.put("status", status);
        afterSummary.put("notifyYn", request.notifyYn());
        afterSummary.put("notifyCreatedCount", notifyCreatedCount);
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.NOTICE_WRITE, AdminAuditLogTargetType.NOTICE,
                notice.getNoticeId(), null, afterSummary);

        return AdminNoticeCreateResponse.from(notice, notifyCreatedCount);
    }

    public AdminNoticeResponse updateNotice(Long adminMemberId, Long noticeId, AdminNoticeSaveRequest request) {
        String status = normalizeRequiredStatus(request.status());
        validateHiddenNotifyCombo(status, request.notifyYn());

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        // notifyYn 값 자체는 요청대로 저장하지만(최종 명세 §API-ADMIN-NOTICE-003), 이 경로에서는
        // fan-out을 호출하지 않는다 — 알림 재발송은 신규 등록에서만 발생한다.
        notice.update(request.title(), request.content(), status, request.notifyYn(), LocalDateTime.now());

        Map<String, Object> afterSummary = new LinkedHashMap<>();
        afterSummary.put("title", notice.getTitle());
        afterSummary.put("status", status);
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.NOTICE_WRITE, AdminAuditLogTargetType.NOTICE,
                notice.getNoticeId(), null, afterSummary);

        return AdminNoticeResponse.from(notice);
    }

    private int fanOutNoticeNotification(Long noticeId, String title) {
        long afterId = 0L;
        int totalNotified = 0;
        Pageable chunk = PageRequest.of(0, FAN_OUT_CHUNK_SIZE);
        List<Long> memberIds;
        do {
            memberIds = memberRepository.findFanOutTargetIds(afterId, chunk);
            if (!memberIds.isEmpty()) {
                notificationService.notifyMemberIdsBatch(memberIds, "NOTICE", title, "NOTICE", noticeId);
                totalNotified += memberIds.size();
                afterId = memberIds.get(memberIds.size() - 1);
            }
        } while (memberIds.size() == FAN_OUT_CHUNK_SIZE);
        return totalNotified;
    }

    private String notificationTitle(String noticeTitle) {
        return "새 공지사항: " + noticeTitle;
    }

    private void validateHiddenNotifyCombo(String status, Boolean notifyYn) {
        if ("HIDDEN".equals(status) && Boolean.TRUE.equals(notifyYn)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String normalizeRequiredStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
