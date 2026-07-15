package com.acorn.abc.member.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberUpdateRequest;
import com.acorn.abc.member.dto.PasswordChangeRequest;
import com.acorn.abc.member.dto.PasswordChangeResponse;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    // 이메일 변경 시 사전 조회(existsByEmailAndMemberIdNot) 이후 동시 요청으로 발생하는
    // uk_member_email UNIQUE 충돌을 409로 변환하기 위한 제약 이름(PR #48 코드리뷰 대응 §4.5).
    private static final String EMAIL_UNIQUE_CONSTRAINT_NAME = "uk_member_email";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MeInfoService meInfoService;

    public PasswordChangeResponse changePassword(Long memberId, PasswordChangeRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_PASSWORD);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        return new PasswordChangeResponse(LocalDateTime.now());
    }

    public MeInfoResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String name = resolveName(member, request.name());
        String email = resolveEmail(member, memberId, request.email());
        String phone = resolvePhone(member, request.phone());
        String gender = resolveGender(member, request.gender());

        member.updateProfile(name, email, phone, gender);
        flushProfileUpdate();

        return meInfoService.getMe(memberId);
    }

    // 사전 조회로 걸러지지 않은, 조회~UPDATE 사이 동시 요청으로 인한 uk_member_email 충돌을
    // 여기서 즉시 확인해 409로 변환한다. 이메일이 아닌 다른 무결성 위반은 예상 범위 밖이므로 그대로 다시 던진다.
    private void flushProfileUpdate() {
        try {
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (isEmailUniqueViolation(e)) {
                throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
            }
            throw e;
        }
    }

    private boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
        Throwable rootCause = e.getMostSpecificCause();
        return rootCause.getMessage() != null && rootCause.getMessage().contains(EMAIL_UNIQUE_CONSTRAINT_NAME);
    }

    private String resolveName(Member member, String requestedName) {
        if (requestedName == null) {
            return member.getName();
        }
        if (requestedName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return requestedName;
    }

    private String resolveEmail(Member member, Long memberId, String requestedEmail) {
        if (requestedEmail == null) {
            return member.getEmail();
        }
        if (requestedEmail.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (!requestedEmail.equals(member.getEmail())
                && memberRepository.existsByEmailAndMemberIdNot(requestedEmail, memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        return requestedEmail;
    }

    private String resolvePhone(Member member, String requestedPhone) {
        if (requestedPhone == null) {
            return member.getPhone();
        }
        return requestedPhone.isBlank() ? null : requestedPhone;
    }

    private String resolveGender(Member member, String requestedGender) {
        if (requestedGender == null) {
            return member.getGender();
        }
        return "NONE".equals(requestedGender) ? null : requestedGender;
    }
}
