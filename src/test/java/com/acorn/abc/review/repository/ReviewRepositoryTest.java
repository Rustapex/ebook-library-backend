package com.acorn.abc.review.repository;

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
class ReviewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RatingRepository ratingRepository;

    private Long bookId;
    private Long memberId1;
    private Long memberId2;

    @BeforeEach
    void setUp() {
        bookId = insertBook();
        memberId1 = insertMember("review_user1");
        memberId2 = insertMember("review_user2");

        insertReview(memberId1, bookId, "ACTIVE", "활성 리뷰");
        insertReview(memberId2, bookId, "DELETED", "삭제된 리뷰");
        insertRating(memberId1, bookId, 5);
        insertRating(memberId2, bookId, 3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findActiveByBookId는_ACTIVE_리뷰만_반환한다() {
        Page<com.acorn.abc.review.domain.Review> page = reviewRepository.findActiveByBookId(bookId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(page.getContent().get(0).getMember().getMemberId()).isEqualTo(memberId1);
    }

    @Test
    void findByMemberIdAndBookId는_상태와_무관하게_회원의_리뷰를_찾는다() {
        var found = reviewRepository.findByMember_MemberIdAndBook_BookId(memberId2, bookId);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("DELETED");
    }

    @Test
    void countByBookId는_해당_도서의_Rating_개수를_반환한다() {
        int count = ratingRepository.countByBook_BookId(bookId);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByBookIdAndMemberIdIn은_지정한_회원들의_Rating만_반환한다() {
        var ratings = ratingRepository.findByBook_BookIdAndMember_MemberIdIn(bookId, java.util.List.of(memberId1));

        assertThat(ratings).hasSize(1);
        assertThat(ratings.get(0).getRatingScore()).isEqualTo((byte) 5);
    }

    // 관리자 통계 확장(PR #42 리뷰 Watchlist W2) — 기간/상태 조건부 집계 검증
    @Test
    void countByStatisticConditions_ACTIVE_상태와_기간_조건을_함께_적용한다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);
        long baseline = reviewRepository.countByStatisticConditions("ACTIVE", from, to, null, null);

        insertReviewAt(memberId1, insertBook(), "ACTIVE", LocalDateTime.of(2026, 1, 15, 10, 0));
        insertReviewAt(memberId1, insertBook(), "ACTIVE", LocalDateTime.of(2026, 2, 1, 0, 0));
        entityManager.flush();
        entityManager.clear();

        long count = reviewRepository.countByStatisticConditions("ACTIVE", from, to, null, null);

        // 1/15 review만 포함, 2/1 00:00:00 review는 endExclusive라 제외 -> 기준선 + 1건
        assertThat(count).isEqualTo(baseline + 1);
    }

    // 회원용독서통계 구현계획 §5.2 — 회원 단위 + 기간 조건부 리뷰 수 집계. 이 테스트에서 새로 만든
    // 회원(statMemberId)만 사용하므로 공유 DB라도 절대값 assertion이 안전하다.
    @Test
    void countByMemberAndPeriod_회원과_기간으로_ACTIVE_리뷰_수를_집계한다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);

        Long statMemberId = insertMember("review_stat_reader");
        insertReviewAt(statMemberId, insertBook(), "ACTIVE", LocalDateTime.of(2026, 1, 15, 10, 0));
        insertReviewAt(statMemberId, insertBook(), "ACTIVE", LocalDateTime.of(2026, 2, 1, 0, 0));
        insertReviewAt(statMemberId, insertBook(), "DELETED", LocalDateTime.of(2026, 1, 20, 0, 0));

        entityManager.flush();
        entityManager.clear();

        long count = reviewRepository.countByMemberAndPeriod(statMemberId, "ACTIVE", from, to);

        // 1/15 ACTIVE만 포함, 2/1 00:00:00(endExclusive)과 DELETED 상태는 제외
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void countByMemberAndPeriod_다른_회원의_리뷰는_포함하지_않는다() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);

        Long targetMemberId = insertMember("review_isolation_target");
        Long otherMemberId = insertMember("review_isolation_other");
        insertReviewAt(targetMemberId, insertBook(), "ACTIVE", LocalDateTime.of(2026, 1, 15, 10, 0));
        insertReviewAt(otherMemberId, insertBook(), "ACTIVE", LocalDateTime.of(2026, 1, 15, 10, 0));

        entityManager.flush();
        entityManager.clear();

        long count = reviewRepository.countByMemberAndPeriod(targetMemberId, "ACTIVE", from, to);

        assertThat(count).isEqualTo(1L);
    }

    private void insertReviewAt(Long memberId, Long bookId, String status, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO review (member_id, book_id, content, status, created_at) VALUES (?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, "기간 테스트 리뷰")
                .setParameter(4, status)
                .setParameter(5, createdAt)
                .executeUpdate();
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

    private Long insertBook() {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book (title, rental_type, rental_price, default_rental_days, total_pages, " +
                        "rental_count, average_rating, review_count, status, created_at) " +
                        "VALUES ('테스트 도서', 'FREE', 0, 7, 100, 0, 0.00, 0, 'AVAILABLE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book");
    }

    private void insertReview(Long memberId, Long bookId, String status, String content) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO review (member_id, book_id, content, status, created_at) VALUES (?, ?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, content)
                .setParameter(4, status)
                .setParameter(5, LocalDateTime.now())
                .executeUpdate();
    }

    private void insertRating(Long memberId, Long bookId, int score) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO rating (member_id, book_id, rating_score, created_at) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, score)
                .setParameter(4, LocalDateTime.now())
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
