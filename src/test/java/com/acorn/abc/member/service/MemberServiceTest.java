package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberUpdateRequest;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

    @InjectMocks
    private MemberService memberService;

    @Test
    void getMe_회원이_존재하면_기존_정보를_반환한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        MeInfoResponse response = memberService.getMe(MEMBER_ID);

        assertThat(response.loginId()).isEqualTo("member0714a");
        assertThat(response.email()).isEqualTo("member0714a@example.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.status()).isEqualTo("JOINED");
    }

    @Test
    void getMe_회원이_없으면_MEMBER_NOT_FOUND를_던진다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMe(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void updateMyInfo_누락된_필드는_기존값을_유지한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, null, null);

        MeInfoResponse response = memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(response.name()).isEqualTo("회원A");
        assertThat(response.email()).isEqualTo("member0714a@example.com");
        assertThat(response.phone()).isEqualTo("010-0714-0001");
        assertThat(response.gender()).isEqualTo("MALE");
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
    }

    @Test
    void updateMyInfo_이메일이_기존값과_동일하면_중복_검증을_생략한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest(null, member.getEmail(), null, null);

        memberService.updateMyInfo(MEMBER_ID, request);

        verify(memberRepository, never()).existsByEmailAndMemberIdNot(eq(member.getEmail()), eq(MEMBER_ID));
    }

    @Test
    void updateMyInfo_전화번호가_빈문자열이면_null로_정규화한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, "", null);

        MeInfoResponse response = memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(response.phone()).isNull();
    }

    @Test
    void updateMyInfo_성별이_NONE이면_null로_저장한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, null, "NONE");

        MeInfoResponse response = memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(response.gender()).isNull();
    }

    @Test
    void updateMyInfo_모든_필드를_정상값으로_수정한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmailAndMemberIdNot("updated@example.com", MEMBER_ID)).willReturn(false);
        MemberUpdateRequest request = new MemberUpdateRequest("회원A수정", "updated@example.com", "010-0714-9999", "FEMALE");

        MeInfoResponse response = memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(response.name()).isEqualTo("회원A수정");
        assertThat(response.email()).isEqualTo("updated@example.com");
        assertThat(response.phone()).isEqualTo("010-0714-9999");
        assertThat(response.gender()).isEqualTo("FEMALE");
        assertThat(response.loginId()).isEqualTo("member0714a");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 7, 14));
    }

    private Member existingMember() {
        Member member = Member.create(
                "member0714a",
                "hashed-password",
                "회원A",
                "member0714a@example.com",
                "010-0714-0001",
                LocalDate.of(1995, 7, 14),
                "MALE",
                mock(MemberGrade.class)
        );
        return member;
    }
}
