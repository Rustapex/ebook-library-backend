package com.acorn.abc.notification.controller;

import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.notification.dto.NoticeDetailResponse;
import com.acorn.abc.notification.dto.NoticeItemResponse;
import com.acorn.abc.notification.service.NoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoticeControllerTest {

    private final NoticeService noticeService = mock(NoticeService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NoticeController(noticeService)).build();
    }

    @Test
    void getNotices_ACTIVE_목록을_200으로_반환한다() throws Exception {
        when(noticeService.getNotices(0, 10)).thenReturn(PageResponse.of(
                List.of(new NoticeItemResponse(1L, "공지", "내용", LocalDateTime.now())), 0, 10, 1));

        mockMvc.perform(get("/api/v1/notices").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].noticeId").value(1));
    }

    @Test
    void getNoticeDetail_prevNext가_없어도_두_key를_포함한다() throws Exception {
        when(noticeService.getNoticeDetail(1L))
                .thenReturn(new NoticeDetailResponse(1L, "공지", "내용", LocalDateTime.now(), null, null));

        mockMvc.perform(get("/api/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prevNotice").value(nullValue()))
                .andExpect(jsonPath("$.data.nextNotice").value(nullValue()));
    }
}
