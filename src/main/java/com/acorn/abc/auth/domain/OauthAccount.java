package com.acorn.abc.auth.domain;

import com.acorn.abc.common.domain.BaseEntity;
import com.acorn.abc.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 인증 제공자 계정과 ABC 회원의 연결.
 *
 * <p>{@code providerUserId}는 Google의 {@code sub}다. email은 변경될 수 있으므로 식별자로 쓰지 않고
 * {@code providerEmail}은 조회/감사용 snapshot으로만 보관한다.
 *
 * <p>DB에 {@code UNIQUE(provider, provider_user_id)}와 {@code UNIQUE(member_id, provider)}가 있어
 * 같은 외부 계정이 여러 회원에 연결되거나 한 회원에 같은 provider가 중복 연결되지 않는다.
 */
@Entity
@Table(name = "oauth_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_id")
    private Long oauthAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OauthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    public static OauthAccount link(Member member, OauthProvider provider,
                                    String providerUserId, String providerEmail) {
        OauthAccount oauthAccount = new OauthAccount();
        oauthAccount.member = member;
        oauthAccount.provider = provider;
        oauthAccount.providerUserId = providerUserId;
        oauthAccount.providerEmail = providerEmail;
        return oauthAccount;
    }

    /**
     * provider가 알려준 email이 바뀐 경우 snapshot만 갱신한다. 연결 대상 회원은 바꾸지 않는다.
     */
    public void updateProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public boolean isLinkedTo(Long memberId) {
        return member != null && member.getMemberId() != null && member.getMemberId().equals(memberId);
    }
}
