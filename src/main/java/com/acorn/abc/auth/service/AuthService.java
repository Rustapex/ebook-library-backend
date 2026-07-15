package com.acorn.abc.auth.service;

import com.acorn.abc.auth.dto.CheckLoginIdResponse;
import com.acorn.abc.auth.dto.FindIdRequest;
import com.acorn.abc.auth.dto.FindIdResponse;
import com.acorn.abc.auth.dto.LoginRequest;
import com.acorn.abc.auth.dto.LoginResponse;
import com.acorn.abc.auth.dto.SignupRequest;
import com.acorn.abc.auth.dto.SignupResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.repository.MemberGradeRepository;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: 비밀번호 재설정 API는 스펙 확정 후 구현

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberGradeRepository memberGradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_ID_DUPLICATED);
        }
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        // TODO: 기본 등급 정책 확정 전까지 grade_id=1 사용
        MemberGrade defaultGrade = memberGradeRepository.findById(1L)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        Member member = Member.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.email(),
                request.phone(),
                request.birthDate(),
                request.gender(),
                defaultGrade
        );

        return SignupResponse.from(memberRepository.save(member));
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        if (!"JOINED".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // TODO: rememberMe=true 시 만료 연장 정책 확정 후 구현
        // TODO: 생일 쿠폰 발급 (쿠폰 도메인 구현 후)

        String accessToken = jwtTokenProvider.generateToken(member.getMemberId(), member.getRole());
        return LoginResponse.of(accessToken, jwtTokenProvider.getExpiration(), member);
    }

    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        return memberRepository.findByNameAndEmail(request.name(), request.email())
                .map(member -> FindIdResponse.found(member.getLoginId(), member.getCreatedAt()))
                .orElseGet(FindIdResponse::notFound);
    }

    // 중복 확인은 아이디를 예약하지 않는다 — 실제 회원가입의 UNIQUE 제약과 signup()의
    // existsByLoginId 검증이 최종 기준이다(로그인아이디 중복확인 API 구현계획 §2.3, §10).
    @Transactional(readOnly = true)
    public CheckLoginIdResponse checkLoginId(String loginId) {
        boolean exists = memberRepository.existsByLoginId(loginId);
        return new CheckLoginIdResponse(loginId, !exists);
    }
}
