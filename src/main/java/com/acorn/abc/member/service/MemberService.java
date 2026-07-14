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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional(readOnly = true)
    public MeInfoResponse getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MeInfoResponse.from(member);
    }

    public MeInfoResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String name = resolveName(member, request.name());
        String email = resolveEmail(member, memberId, request.email());
        String phone = resolvePhone(member, request.phone());
        String gender = resolveGender(member, request.gender());

        member.updateProfile(name, email, phone, gender);
        return MeInfoResponse.from(member);
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
