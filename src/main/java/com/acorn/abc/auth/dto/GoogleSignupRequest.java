package com.acorn.abc.auth.dto;

import com.acorn.abc.auth.validation.ValidLoginId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 신규 Google 사용자의 추가 가입(onboarding) 입력.
 *
 * <p><b>email은 받지 않는다.</b> 서버가 SIGNUP transition token의 claim에서 가져온다.
 * frontend가 보낸 값으로 다른 사람의 email을 가로챌 수 없게 하기 위함이다.
 *
 * <p>반면 {@code name}은 입력받는다. Google ID token의 {@code name} claim은 {@code profile} scope가
 * 승인됐을 때만 존재하므로, token에만 의존하면 {@code openid email} scope만 승인한 사용자가
 * {@code member.name NOT NULL} 위반으로 영구히 가입할 수 없다. name은 email과 달리 신원 식별자가
 * 아니고 일반 회원가입도 사용자 입력을 그대로 쓰므로, 입력받아 검증하는 것이 안전하고 일관된다.
 * 응답의 {@code transition.name}은 화면 prefill용이다.
 *
 * <p>{@code member} table의 필수 column에 synthetic 값을 만들지 않는다는 정책에 따라
 * loginId/password/birthDate를 실제로 입력받는다. 검증 계약은 일반 회원가입과 동일하다.
 */
public record GoogleSignupRequest(

        @NotBlank(message = "인증 정보가 필요합니다.")
        String transitionToken,

        @ValidLoginId
        String loginId,

        // member.name은 VARCHAR(50) NOT NULL이다. 길이를 넘기면 DB 제약 위반이 되므로 여기서 막는다.
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
        String name,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해 8~20자로 입력해주세요."
        )
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        // member.phone은 VARCHAR(20)이다. 초과 입력이 DB 제약 위반으로 흘러가지 않게 여기서 막는다.
        @Size(max = 20, message = "연락처는 20자 이하로 입력해주세요.")
        String phone,

        @NotNull(message = "생년월일은 필수입니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @NotBlank(message = "성별은 필수입니다.")
        @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE, FEMALE 중 하나여야 합니다.")
        String gender
) {}
