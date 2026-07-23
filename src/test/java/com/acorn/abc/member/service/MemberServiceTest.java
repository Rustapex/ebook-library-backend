package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberGradeSummaryResponse;
import com.acorn.abc.member.dto.MemberUpdateRequest;
import com.acorn.abc.member.dto.MemberWithdrawRequest;
import com.acorn.abc.member.dto.MemberWithdrawResponse;
import com.acorn.abc.member.dto.PasswordChangeRequest;
import com.acorn.abc.member.dto.PasswordChangeResponse;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
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
    private BookRentalRepository bookRentalRepository;

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
    void updateMyInfo_성별_요청값과_무관하게_기존_성별을_유지한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubMeInfoResponse());
        MemberUpdateRequest request = new MemberUpdateRequest(null, null, null, "FEMALE");

        memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(member.getGender()).isEqualTo("MALE");
    }

    @Test
    void updateMyInfo_이름_이메일_전화번호를_정상값으로_수정하고_MeInfoService_응답을_그대로_반환한다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmailAndMemberIdNot("updated@example.com", MEMBER_ID)).willReturn(false);
        MeInfoResponse stubResponse = stubMeInfoResponse();
        given(meInfoService.getMe(MEMBER_ID)).willReturn(stubResponse);
        MemberUpdateRequest request = new MemberUpdateRequest("회원A수정", "updated@example.com", "010-0714-9999", null);

        MeInfoResponse response = memberService.updateMyInfo(MEMBER_ID, request);

        assertThat(member.getName()).isEqualTo("회원A수정");
        assertThat(member.getEmail()).isEqualTo("updated@example.com");
        assertThat(member.getPhone()).isEqualTo("010-0714-9999");
        assertThat(member.getGender()).isEqualTo("MALE");
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

    @Test
    void changePassword_현재_비밀번호가_일치하지_않으면_AUTH_INVALID_PASSWORD를_던진다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong-password", member.getPasswordHash())).willReturn(false);
        PasswordChangeRequest request = new PasswordChangeRequest("wrong-password", "NewPassw0rd!!", "NewPassw0rd!!");

        assertThatThrownBy(() -> memberService.changePassword(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_PASSWORD);
        assertThat(member.getPasswordHash()).isEqualTo("hashed-password");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePassword_새_비밀번호가_현재_비밀번호와_같으면_VALIDATION_ERROR를_던지고_변경하지_않는다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("Passw0rd!!", member.getPasswordHash())).willReturn(true);
        PasswordChangeRequest request = new PasswordChangeRequest("Passw0rd!!", "Passw0rd!!", "Passw0rd!!");

        assertThatThrownBy(() -> memberService.changePassword(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(member.getPasswordHash()).isEqualTo("hashed-password");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePassword_새_비밀번호가_현재_비밀번호와_다르면_정상적으로_변경된다() {
        Member member = existingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("Passw0rd!!", member.getPasswordHash())).willReturn(true);
        given(passwordEncoder.matches("NewPassw0rd!!", member.getPasswordHash())).willReturn(false);
        given(passwordEncoder.encode("NewPassw0rd!!")).willReturn("new-hashed-password");
        PasswordChangeRequest request = new PasswordChangeRequest("Passw0rd!!", "NewPassw0rd!!", "NewPassw0rd!!");

        PasswordChangeResponse response = memberService.changePassword(MEMBER_ID, request);

        assertThat(member.getPasswordHash()).isEqualTo("new-hashed-password");
        assertThat(response).isNotNull();
    }

    @Test
    void withdraw_ADMIN_계정이면_AUTH_ACCESS_DENIED를_던진다() {
        Member admin = mock(Member.class);
        given(admin.isAdmin()).willReturn(true);
        given(memberRepository.findByIdForUpdate(MEMBER_ID)).willReturn(Optional.of(admin));
        MemberWithdrawRequest request = new MemberWithdrawRequest("Passw0rd!!", null);

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(bookRentalRepository, never()).existsByMember_MemberIdAndRentalStatusIn(any(), any());
    }

    // PR64 코드리뷰 대응 §2.1 — 잠금 획득 사이 다른 트랜잭션이 먼저 상태를 바꾼 경합 상황(예: 이중 탈퇴 요청)을 방어한다.
    @Test
    void withdraw_JOINED_상태가_아니면_AUTH_ACCESS_DENIED를_던진다() {
        Member member = mock(Member.class);
        given(member.isAdmin()).willReturn(false);
        given(member.getStatus()).willReturn("WITHDRAWN");
        given(memberRepository.findByIdForUpdate(MEMBER_ID)).willReturn(Optional.of(member));
        MemberWithdrawRequest request = new MemberWithdrawRequest("Passw0rd!!", null);

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(bookRentalRepository, never()).existsByMember_MemberIdAndRentalStatusIn(any(), any());
    }

    @Test
    void withdraw_현재_비밀번호가_일치하지_않으면_AUTH_INVALID_PASSWORD를_던진다() {
        Member member = existingMember();
        given(memberRepository.findByIdForUpdate(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong-password", member.getPasswordHash())).willReturn(false);
        MemberWithdrawRequest request = new MemberWithdrawRequest("wrong-password", null);

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_PASSWORD);
        assertThat(member.getStatus()).isEqualTo("JOINED");
        verify(bookRentalRepository, never()).existsByMember_MemberIdAndRentalStatusIn(any(), any());
    }

    @Test
    void withdraw_진행중인_대여가_있으면_MEMBER_WITHDRAWAL_RENTAL_IN_PROGRESS를_던진다() {
        Member member = existingMember();
        given(memberRepository.findByIdForUpdate(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("Passw0rd!!", member.getPasswordHash())).willReturn(true);
        given(bookRentalRepository.existsByMember_MemberIdAndRentalStatusIn(MEMBER_ID, List.of("READY", "READING")))
                .willReturn(true);
        MemberWithdrawRequest request = new MemberWithdrawRequest("Passw0rd!!", null);

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_WITHDRAWAL_RENTAL_IN_PROGRESS);
        assertThat(member.getStatus()).isEqualTo("JOINED");
    }

    @Test
    void withdraw_진행중인_대여가_없으면_WITHDRAWN으로_전환하고_탈퇴일시와_사유를_저장한다() {
        Member member = existingMember();
        given(memberRepository.findByIdForUpdate(MEMBER_ID)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("Passw0rd!!", member.getPasswordHash())).willReturn(true);
        given(bookRentalRepository.existsByMember_MemberIdAndRentalStatusIn(MEMBER_ID, List.of("READY", "READING")))
                .willReturn(false);
        MemberWithdrawRequest request = new MemberWithdrawRequest("Passw0rd!!", "더 이상 사용하지 않음");

        MemberWithdrawResponse response = memberService.withdraw(MEMBER_ID, request);

        assertThat(member.getStatus()).isEqualTo("WITHDRAWN");
        assertThat(member.getWithdrawnAt()).isNotNull();
        assertThat(member.getWithdrawalReason()).isEqualTo("더 이상 사용하지 않음");
        assertThat(response.status()).isEqualTo("WITHDRAWN");
        assertThat(response.withdrawnAt()).isEqualTo(member.getWithdrawnAt());
        verify(bookRentalRepository, never()).save(any());
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
