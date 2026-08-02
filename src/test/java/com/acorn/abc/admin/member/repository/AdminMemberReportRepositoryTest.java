package com.acorn.abc.admin.member.repository;

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

/**
 * PR51 코드리뷰 대응(2026-07-18) §2.3-3 — REPORTS 탭 UNION ALL 네이티브 쿼리를 실제 MySQL로 검증한다.
 * 목적: book_report/review_report를 메모리에서 합치지 않고도 정렬·페이지가 DB 단계에서 정확히 적용되는지 확인.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminMemberReportRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AdminMemberReportRepository adminMemberReportRepository;

    private Long reporterId;

    @BeforeEach
    void setUp() {
        reporterId = insertMember("report_query_user");
        Long bookId = insertBook();
        Long reviewId = insertReview(reporterId, bookId);
        Long otherReporterId = insertMember("report_query_other");

        insertBookReport(reporterId, bookId, LocalDateTime.of(2026, 7, 1, 0, 0));
        insertReviewReport(reporterId, reviewId, LocalDateTime.of(2026, 7, 3, 0, 0));
        insertBookReport(reporterId, bookId, LocalDateTime.of(2026, 7, 2, 0, 0));
        insertBookReport(otherReporterId, bookId, LocalDateTime.of(2026, 7, 5, 0, 0));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByReporterId_두_테이블을_createdAt_내림차순으로_합치고_다른_회원의_신고는_제외한다() {
        Page<AdminMemberReportRepository.AdminMemberReportRow> page =
                adminMemberReportRepository.findByReporterId(reporterId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(AdminMemberReportRepository.AdminMemberReportRow::getReportSourceType)
                .containsExactly("REVIEW_REPORT", "BOOK_REPORT", "BOOK_REPORT");
    }

    @Test
    void findByReporterId_페이지_크기만큼만_반환하고_전체_건수는_유지한다() {
        Page<AdminMemberReportRepository.AdminMemberReportRow> firstPage =
                adminMemberReportRepository.findByReporterId(reporterId, PageRequest.of(0, 2));
        Page<AdminMemberReportRepository.AdminMemberReportRow> secondPage =
                adminMemberReportRepository.findByReporterId(reporterId, PageRequest.of(1, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getContent()).hasSize(1);
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
                        "VALUES ('신고 테스트 도서', 'FREE', 0, 7, 100, 0, 0.00, 0, 'AVAILABLE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("book");
    }

    private Long insertReview(Long memberId, Long bookId) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO review (member_id, book_id, content, status, created_at) " +
                        "VALUES (?, ?, '리뷰 내용', 'ACTIVE', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, memberId)
                .setParameter(2, bookId)
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();
        return lastInsertId("review");
    }

    private void insertBookReport(Long reporterId, Long bookId, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO book_report (reporter_id, book_id, report_type, content, status, created_at) " +
                        "VALUES (?, ?, 'SPOILER', '신고 내용', 'WAITING', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, reporterId)
                .setParameter(2, bookId)
                .setParameter(3, createdAt)
                .executeUpdate();
    }

    private void insertReviewReport(Long reporterId, Long reviewId, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO review_report (reporter_id, review_id, report_type, content, status, created_at) " +
                        "VALUES (?, ?, 'ABUSE', '신고 내용', 'WAITING', ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, reporterId)
                .setParameter(2, reviewId)
                .setParameter(3, createdAt)
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
