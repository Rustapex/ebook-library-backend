package com.acorn.abc.auth.repository;

import com.acorn.abc.auth.domain.OauthAccount;
import com.acorn.abc.auth.domain.OauthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {

    // GoogleAuthService는 transaction 밖에서 member 상태를 읽으므로 LAZY 연관을 함께 가져온다.
    // fetch join이 없으면 getMember() 접근에서 LazyInitializationException이 발생한다.
    @Query("SELECT oa FROM OauthAccount oa JOIN FETCH oa.member "
            + "WHERE oa.provider = :provider AND oa.providerUserId = :providerUserId")
    Optional<OauthAccount> findByProviderAndProviderUserId(@Param("provider") OauthProvider provider,
                                                           @Param("providerUserId") String providerUserId);

    boolean existsByMemberMemberIdAndProvider(Long memberId, OauthProvider provider);
}
