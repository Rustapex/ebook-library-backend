package com.acorn.abc.notification.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.notification.domain.Notice;
import com.acorn.abc.notification.dto.NoticeDetailResponse;
import com.acorn.abc.notification.dto.NoticeItemResponse;
import com.acorn.abc.notification.dto.NoticeListCriteria;
import com.acorn.abc.notification.dto.NoticeNeighborResponse;
import com.acorn.abc.notification.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final String ACTIVE = "ACTIVE";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final NoticeRepository noticeRepository;

    public PageResponse<NoticeItemResponse> getNotices(Integer page, Integer size) {
        NoticeListCriteria criteria = new NoticeListCriteria(normalizePage(page), normalizeSize(size));
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
        Page<Notice> notices = noticeRepository.findByNoticeStatusOrderByCreatedAtDescNoticeIdDesc(ACTIVE, pageable);
        List<NoticeItemResponse> content = notices.getContent().stream().map(NoticeItemResponse::from).toList();
        return PageResponse.of(content, notices.getNumber(), notices.getSize(), notices.getTotalElements());
    }

    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findByNoticeIdAndNoticeStatus(noticeId, ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        NoticeNeighborResponse prevNotice = noticeRepository.findPrevActive(notice.getCreatedAt(), notice.getNoticeId())
                .map(NoticeNeighborResponse::from)
                .orElse(null);
        NoticeNeighborResponse nextNotice = noticeRepository.findNextActive(notice.getCreatedAt(), notice.getNoticeId())
                .map(NoticeNeighborResponse::from)
                .orElse(null);

        return new NoticeDetailResponse(notice.getNoticeId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt(),
                prevNotice, nextNotice);
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
