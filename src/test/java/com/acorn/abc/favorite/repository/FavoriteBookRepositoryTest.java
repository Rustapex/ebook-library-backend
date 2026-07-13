package com.acorn.abc.favorite.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FavoriteBookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FavoriteBookRepository favoriteBookRepository;

    private Long memberId;
    private Long bookId1;
    private Long bookId2;

    @BeforeEach
    void setUp() {
        memberId = insertMember("favorite_user");
        bookId1 = insertBook("도서1");
        bookId2 = insertBook("도서2");

        insertFavorite(memberId, bookId1);
        insertFavorite(memberId, bookId2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findMyFavorites는_회원의_즐겨찾기를_최신순으로_페이징해서_반환한다() {
        Page<com.acorn.abc.favorite.domain.FavoriteBook> page = favoriteBookRepository.findMyFavorites(memberId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(fb -> fb.getBook().getBookId())
                .containsExactly(bookId2, bookId1);
    }

    @Test
    void findByMemberIdAndBookId는_존재하면_반환한다() {
        var found = favoriteBookRepository.findByMember_MemberIdAndBook_BookId(memberId, bookId1);

        assertThat(found).isPresent();
    }

    @Test
    void findByMemberIdAndBookId는_존재하지_않으면_빈값을_반환한다() {
        var found = favoriteBookRepository.findByMember_MemberIdAndBook_BookId(memberId, 999999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findMyFavorites는_HIDDEN_INACTIVE_도서를_목록에서_제외한다() {
        Long hiddenBookId = insertBook("숨김 도서", "HIDDEN");
        Long inactiveBookId = insertBook("비활성 도서", "INACTIVE");
        insertFavorite(memberId, hiddenBookId);
        insertFavorite(memberId, inactiveBookId);
        entityManager.flush();
        entityManager.clear();

        Page<com.acorn.abc.favorite.domain.FavoriteBook> page = favoriteBookRepository.findMyFavorites(memberId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(fb -> fb.getBook().getBookId())
                .doesNotContain(hiddenBookId, inactiveBookId)
                .containsExactly(bookId2, bookId1);
    }

    @Test
    void findByMemberIdAndBookId는_HIDDEN_INACTIVE_도서여도_삭제를_위해_조회된다() {
        Long hiddenBookId = insertBook("숨김 도서", "HIDDEN");
        insertFavorite(memberId, hiddenBookId);
        entityManager.flush();
        entityManager.clear();

        var found = favoriteBookRepository.findByMember_MemberIdAndBook_BookId(memberId, hiddenBookId);

        assertThat(found).isPresent();
    }

    // 회원용독서통계 구현계획 §4.4/§5.2 — 즐겨찾기 수는 현재 상태 기준(기간 조건 없음). setUp()에서
    // 만든 memberId(이 테스트 전용) 데이터만 사용하므로 공유 DB라도 절대값 assertion이 안전하다.
    @Test
    void countByMemberId_현재_즐겨찾기_수를_반환한다() {
        long count = favoriteBookRepository.countByMember_MemberId(memberId);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countByMemberId_다른_회원의_즐겨찾기는_포함하지_않는다() {
        Long otherMemberId = insertMember("favorite_stat_other");
        insertFavorite(otherMemberId, insertBook("도서3"));
        entityManager.flush();
        entityManager.clear();

        long count = favoriteBookRepository.countByMember_MemberId(memberId);

        assertThat(count).isEqualTo(2L);
    }

    private Long insertMember(String loginId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, point_balance, status, created_at) " +
                        "VALUES (?, 'hash', 'Tester', ?, '2000-01-01', 'USER', 1, 0, 'JOINED', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, loginId)
                .setParameter(2, loginId + "@test.com")
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("member");
    }

    private Long insertBook(String title) {
        return insertBook(title, "AVAILABLE");
    }

    private Long insertBook(String title, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (title, rental_type, rental_price, default_rental_days, total_pages, " +
                        "rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES (?, 'FREE', 0, 7, 100, 0, 0.00, 0, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, title)
                .setParameter(2, status)
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book");
    }

    private void insertFavorite(Long memberId, Long bookId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO favorite_book (member_id, book_id, created_at) VALUES (?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
    }

    private Long lastInsertId(String table) {
        String idColumn = table + "_id";
        Number number = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT MAX(" + idColumn + ") FROM " + table)
                .getSingleResult();
        return number.longValue();
    }
}
