package com.acorn.abc.admin.member.dto;

// 관리자 회원 상세(API-ADMIN-MEMBER-002) 탭 구분(구현계획 §1.3). 값이 없으면 컨트롤러에서 DEFAULT로 취급한다.
public enum AdminMemberDetailTab {
    DEFAULT,
    RENTALS,
    PAYMENTS,
    REPORTS,
    POINTS,
    SANCTIONS
}
