package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberGradeSummaryResponse;
import com.acorn.abc.member.dto.MemberUpdateRequest;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MeInfoService meInfoService;

    @InjectMocks
    private MemberService memberService;

    @Test
    void updateMyInfo_누락된_필드는_기존값을_유지한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubMeInfoResponse());
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, null, null);

        memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(member.getName()).isEqualTo("회원A");
        assertThat(member.getEmail()).isEqualTo("member0714a@example.com");
        assertThat(member.getPhone()).isEqualTo("010-0714-0001");
        assertThat(member.getGender()).isEqualTo("MALE");
        verify(memberRepository, never()).existsByEmailAndMemberIdNot(any(), anyLong());
    }

    @Test
    void updateMyInfo_이름이_공백이면_VALIDATION_ERROR를_던진다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest(" ", null, null, null);

        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateMyInfo_이메일이_공백이면_VALIDATION_ERROR를_던진다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest(null, " ", null, null);

        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateMyInfo_다른_회원의_이메일과_중복되면_MEMBER_EMAIL_DUPLICATED를_던진다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmailAndMemberIdNot("other@example.com", MEMBER_ID)).willReturn(true);
        MemberUpdateRequest request = new MemberUpdateRequest(null, "other@example.com", null, null);

        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        verify(memberRepository, never()).flush();
    }

    @Test
    void updateMyInfo_이메일이_기존값과_동일하면_중복_검증을_생략한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubMeInfoResponse());
        MemberUpdateRequest request = new MemberUpdateRequest(null, member.getEmail(), null, null);

        memberService.updateMyInfo(MEMBER_ID, request);

        verify(memberRepository, never()).existsByEmailAndMemberIdNot(eq(member.getEmail()), eq(MEMBER_ID));
    }

    @Test
    void updateMyInfo_전화번호가_빈문자열이면_null로_정규화한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubMeInfoResponse());
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, "", null);

        memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(member.getPhone()).isNull();
    }

    @Test
    void updateMyInfo_성별이_NONE이면_null로_저장한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubMeInfoResponse());
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, null, "NONE");

        memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(member.getGender()).isNull();
    }

    @Test
    void updateMyInfo_모든_필드를_정상값으로_수정하고_MeInfoService_응답을_그대로_반환한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmailAndMemberIdNot("updated@example.com", MEMBER_ID)).willReturn(false);
        MeInfoResponse stubResponse = stubMeInfoResponse();
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubResponse);
        MemberUpdateRequest request = new MemberUpdateRequest("회원A수정", "updated@example.com", "010-0714-9999", "FEMALE");

        MeInfoResponse response = memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(member.getName()).isEqualTo("회원A수정");
        assertThat(member.getEmail()).isEqualTo("updated@example.com");
        assertThat(member.getPhone()).isEqualTo("010-0714-9999");
        assertThat(member.getGender()).isEqualTo("FEMALE");
        assertThat(response).isSameAs(stubResponse);
    }

    @Test
    void updateMyInfo_사전조회_이후_flush에서_이메일_UNIQUE_충돌이_발생하면_MEMBER_EMAIL_DUPLICATED로_변환한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmailAndMemberIdNot("race@example.com", MEMBER_ID)).willReturn(false);
        doThrow(new DataIntegrityViolationException("Duplicate entry 'race@example.com' for key 'member.uk_member_email'"))
                .when(memberRepository).flush();
        MemberUpdateRequest request = new MemberUpdateRequest(null, "race@example.com", null, null);

        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
    }

    @Test
    void updateMyInfo_이메일이_아닌_다른_무결성_위반은_그대로_다시_던진다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        DataIntegrityViolationException original =
                new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails");
        doThrow(original).when(memberRepository).flush();
        MemberUpdateRequest request = new MemberUpdateRequest("정상이름", null, null, null);

        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_ID, request))
                .isSameAs(original);
    }

    private MeInfoResponse stubMeInfoResponse() {
        return new MeInfoResponse(
                MEMBER_ID, "member0714a", "회원A수정", "updated@example.com", "010-0714-9999",
                LocalDate.of(1995, 7, 14), "FEMALE", "USER", "JOINED",
                new MemberGradeSummaryResponse(1L, "씨앗", 1, 2L, "새싹", 10000L, 20000L, 50.0),
                3000, 2, 5, 3, 4, 1);
    }

    private Member existingMember() {
        return Member.create(
                "member0714a",
                "hashed-password",
                "회원A",
                "member0714a@example.com",
                "010-0714-0001",
                LocalDate.of(1995, 7, 14),
                "MALE",
                mock(MemberGrade.class)
        );
    }
}
