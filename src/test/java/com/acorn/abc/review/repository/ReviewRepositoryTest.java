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
