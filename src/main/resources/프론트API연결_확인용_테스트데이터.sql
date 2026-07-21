-- 2026-07-20 프론트-백엔드 API 연결 확인용 수동 테스트 데이터 (sidecar SQL, schema.sql/data.sql 미수정)
-- 관련 작업: docs/작업현황/2026-07-20_백엔드O-화면O-프론트연결X_API_연결계획.md
--
-- 목적:
--   14개 API(사용자 컬렉션 2, 관리자 도서 5, 관리자 컬렉션 4, 관리자 회원 3) 연결 후
--   프론트 하드코딩 fallback을 제거했기 때문에, 화면에 실제 데이터가 정상적으로
--   뿌려지는지 눈으로 확인하려면 DB에 최소한의 실 데이터가 있어야 한다.
--   data.sql에는 member_grade 외 시드가 없으므로 이 파일로 별도 삽입한다.
--
-- 적용 정책:
--   - schema.sql/data.sql은 이 작업으로 수정하지 않는다. 이 파일은 수동 적용 sidecar SQL이다.
--   - 로컬 개발 DB에서 먼저 실행할 것을 권장한다. 공유 RDS에서 실행하려면 실행 전
--     §0으로 기존 데이터 유무를 먼저 확인하고, 팀원과 충돌하지 않는지 확인한다.
--   - 모든 테스트 행은 login_id `apitest_%`, isbn `99900000%`, 이름/제목에
--     `API연결테스트` 접두어로 표시해 실데이터와 구분하고, 맨 아래 §17 원복 스크립트로
--     정확히 이 행들만 삭제할 수 있게 했다.
--   - password_hash는 실제 로그인용이 아닌 더미 값이다. 이 회원으로 로그인 테스트는 불가하다.
--
-- 확인 대상 화면:
--   U-001/U-006(사용자 컬렉션), A-004/005/006(관리자 도서), A-018(관리자 컬렉션),
--   A-002/003(관리자 회원: 목록/상세/탭 5종/상태변경/포인트조정)

-- §0. 현재 DB와 기존 테스트 데이터 유무 확인 (실행 전 반드시 확인)
SELECT DATABASE() AS current_database;
SELECT COUNT(*) AS existing_apitest_members FROM member WHERE login_id LIKE 'apitest_%';
SELECT COUNT(*) AS existing_apitest_books FROM book WHERE isbn LIKE '99900000%';

-- ============================================================
-- §1. 출판사
-- ============================================================
INSERT INTO publisher (publisher_name, status, created_at)
VALUES ('API연결테스트 출판사', 'ACTIVE', NOW());
SET @pub1 = LAST_INSERT_ID();

-- ============================================================
-- §2. 저자 3명 (도서1에 3명을 모두 매핑해 관리자 도서 목록의 저자 여러 명 표시를 확인한다)
-- ============================================================
INSERT INTO author (author_name, status, created_at) VALUES ('API연결테스트 저자1', 'ACTIVE', NOW());
SET @author1 = LAST_INSERT_ID();
INSERT INTO author (author_name, status, created_at) VALUES ('API연결테스트 저자2', 'ACTIVE', NOW());
SET @author2 = LAST_INSERT_ID();
INSERT INTO author (author_name, status, created_at) VALUES ('API연결테스트 저자3', 'ACTIVE', NOW());
SET @author3 = LAST_INSERT_ID();

-- ============================================================
-- §3. 카테고리 3개 (도서1에 3개를 모두 매핑해 카테고리 여러 개 표시를 확인한다)
-- ============================================================
INSERT INTO category (category_name, display_order, status) VALUES ('API연결테스트 카테고리1', 1, 'ACTIVE');
SET @cat1 = LAST_INSERT_ID();
INSERT INTO category (category_name, display_order, status) VALUES ('API연결테스트 카테고리2', 2, 'ACTIVE');
SET @cat2 = LAST_INSERT_ID();
INSERT INTO category (category_name, display_order, status) VALUES ('API연결테스트 카테고리3', 3, 'ACTIVE');
SET @cat3 = LAST_INSERT_ID();

-- ============================================================
-- §4. 도서 5권
--   도서1: 저자3명+카테고리3개(관리자 도서 목록 셀 오버플로우 확인용), 상세 텍스트 포함, AVAILABLE
--   도서2: 저자1명+카테고리1개, HIDDEN
--   도서3: 저자2명+카테고리2개, INACTIVE
--   도서4·5: 컬렉션 매핑용, AVAILABLE
-- ============================================================
INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, default_rental_days, status, created_at)
VALUES (@pub1, 'API연결테스트 도서1(저자·카테고리 다수)', '9990000000001', 'PAID', 3000, 14, 'AVAILABLE', NOW());
SET @book1 = LAST_INSERT_ID();

INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, default_rental_days, status, created_at)
VALUES (@pub1, 'API연결테스트 도서2(숨김)', '9990000000002', 'FREE', 0, 14, 'HIDDEN', NOW());
SET @book2 = LAST_INSERT_ID();

INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, default_rental_days, status, created_at)
VALUES (@pub1, 'API연결테스트 도서3(비활성)', '9990000000003', 'PAID', 4500, 14, 'INACTIVE', NOW());
SET @book3 = LAST_INSERT_ID();

INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, default_rental_days, status, created_at)
VALUES (@pub1, 'API연결테스트 도서4(컬렉션A)', '9990000000004', 'PAID', 2500, 14, 'AVAILABLE', NOW());
SET @book4 = LAST_INSERT_ID();

INSERT INTO book (publisher_id, title, isbn, rental_type, rental_price, default_rental_days, status, created_at)
VALUES (@pub1, 'API연결테스트 도서5(컬렉션B)', '9990000000005', 'FREE', 0, 14, 'AVAILABLE', NOW());
SET @book5 = LAST_INSERT_ID();

-- §5. 저자 매핑
INSERT INTO book_author (book_id, author_id, author_order) VALUES
  (@book1, @author1, 1), (@book1, @author2, 2), (@book1, @author3, 3),
  (@book2, @author1, 1),
  (@book3, @author2, 1), (@book3, @author3, 2),
  (@book4, @author1, 1),
  (@book5, @author2, 1);

-- §6. 카테고리 매핑
INSERT INTO book_category (book_id, category_id) VALUES
  (@book1, @cat1), (@book1, @cat2), (@book1, @cat3),
  (@book2, @cat1),
  (@book3, @cat2), (@book3, @cat3),
  (@book4, @cat1),
  (@book5, @cat2);

-- §7. 도서1 상세(A-006 수정 화면의 소개/목차/출판사 리뷰 확인용)
INSERT INTO book_detail (book_id, description, table_of_contents, publisher_review, created_at)
VALUES (
  @book1,
  'API 연결 확인용 책 소개입니다. 실제 데이터가 화면에 그대로 반영되는지 확인하는 용도입니다.',
  '1장 API 연결 확인\n2장 관리자 도서 화면 확인\n3장 마무리',
  'API 연결 확인용 출판사 리뷰입니다.',
  NOW()
);

-- ============================================================
-- §8. 회원 3명
--   member1: SANCTIONED, 대여/결제/리뷰/포인트/제재 이력 전부 보유(탭 5종 확인용)
--   member2: JOINED, 신고자 역할(제재/이력에는 등장하지 않음)
--   member3: JOINED, 이력 없음(빈 결과 상태 확인용)
-- ============================================================
INSERT INTO member (login_id, password_hash, name, email, phone, birth_date, gender, role, grade_id, point_balance, status, created_at)
VALUES ('apitest_member1', 'TEST_ONLY_NOT_A_REAL_HASH', 'API연결테스트 회원1', 'apitest_member1@example.com', '010-0000-0001', '1995-01-01', 'F', 'USER', 1, 1600, 'SANCTIONED', NOW());
SET @member1 = LAST_INSERT_ID();

INSERT INTO member (login_id, password_hash, name, email, phone, birth_date, gender, role, grade_id, point_balance, status, created_at)
VALUES ('apitest_member2', 'TEST_ONLY_NOT_A_REAL_HASH', 'API연결테스트 회원2(신고자)', 'apitest_member2@example.com', '010-0000-0002', '1993-05-05', 'M', 'USER', 1, 0, 'JOINED', NOW());
SET @member2 = LAST_INSERT_ID();

INSERT INTO member (login_id, password_hash, name, email, phone, birth_date, gender, role, grade_id, point_balance, status, created_at)
VALUES ('apitest_member3', 'TEST_ONLY_NOT_A_REAL_HASH', 'API연결테스트 회원3(이력없음)', 'apitest_member3@example.com', '010-0000-0003', '1998-09-09', 'F', 'USER', 2, 500, 'JOINED', NOW());
SET @member3 = LAST_INSERT_ID();

-- §9. 대여 이력 (member1, 대여 탭 확인용)
INSERT INTO book_rental (member_id, book_id, rental_start_at, rental_end_at, rental_status, created_at)
VALUES (@member1, @book1, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 'READING', NOW());
SET @rental1 = LAST_INSERT_ID();

INSERT INTO book_rental (member_id, book_id, rental_start_at, rental_end_at, rental_status, created_at)
VALUES (@member1, @book4, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 'OWNED', NOW());
SET @rental2 = LAST_INSERT_ID();

-- §10. 결제 이력 (member1, 결제 탭 확인용)
INSERT INTO payment (member_id, rental_id, payment_type, original_amount, amount, payment_method, payment_status, paid_at, created_at)
VALUES (@member1, @rental1, 'RENTAL', 3000, 3000, 'CARD', 'PAID', NOW(), NOW());
SET @payment1 = LAST_INSERT_ID();

INSERT INTO payment (member_id, rental_id, payment_type, original_amount, amount, payment_method, payment_status, paid_at, created_at)
VALUES (@member1, @rental2, 'RENTAL', 2500, 2500, 'CARD', 'PAID', NOW(), NOW());
SET @payment2 = LAST_INSERT_ID();

-- §11. 리뷰 (member1이 도서1에 작성 → member2가 신고 → member1 제재의 근거로 연결)
INSERT INTO review (member_id, book_id, content, status, created_at)
VALUES (@member1, @book1, 'API 연결 테스트용 리뷰입니다.', 'ACTIVE', NOW());
SET @review1 = LAST_INSERT_ID();

-- §12. 리뷰 신고 (신고 탭 확인용, reportSourceType=REVIEW_REPORT)
INSERT INTO review_report (reporter_id, review_id, report_type, content, status, processed_at, process_result, created_at)
VALUES (@member2, @review1, 'SPOILER', 'API 연결 테스트 - 스포일러 포함 신고', 'DONE', NOW(), 'API 연결 테스트 - 경고 조치 완료', NOW());
SET @review_report1 = LAST_INSERT_ID();

-- §13. 도서 신고 (신고 탭 확인용, reportSourceType=BOOK_REPORT, 처리 전 상태)
INSERT INTO book_report (reporter_id, book_id, report_type, content, status, created_at)
VALUES (@member2, @book2, 'WRONG_INFO', 'API 연결 테스트 - 도서 정보 오류 신고', 'WAITING', NOW());

-- §14. 제재 이력 (member1, §12의 리뷰 신고를 근거로 계정 정지 — 회원 목록/상세의 활성 제재 확인용)
INSERT INTO sanction_history (member_id, review_report_id, sanction_type, reason, started_at, ended_at, created_at)
VALUES (@member1, @review_report1, 'ACCOUNT_SUSPENSION', 'API 연결 테스트 - 리뷰 신고 누적으로 인한 계정 정지', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW());

-- §15. 포인트 이력 (member1, 포인트 탭 및 잔액 확인용 — member.point_balance=1600과 합계를 맞춤)
INSERT INTO point_history (member_id, payment_id, point_type, point_amount, description, created_at)
VALUES (@member1, NULL, 'ADMIN_ADJUST', 1500, 'API 연결 테스트 - 관리자 지급', NOW());
INSERT INTO point_history (member_id, payment_id, point_type, point_amount, description, created_at)
VALUES (@member1, @payment1, 'PAYBACK', 100, 'API 연결 테스트 - 결제 적립', NOW());

-- ============================================================
-- §16. 컬렉션 2개 (사용자 U-001/U-006, 관리자 A-018 확인용)
--   컬렉션A: SERIES → 도서별 display_order 필수(양수, 중복 불가)
--   컬렉션B: EVENT  → 도서별 display_order는 NULL이어야 한다(스키마·서비스 규칙)
-- ============================================================
INSERT INTO collection (name, collection_type, discount_rate, description, start_date, end_date, status, created_at)
VALUES ('API연결테스트 시리즈 컬렉션', 'SERIES', NULL, 'API 연결 테스트용 시리즈 컬렉션입니다.', NULL, NULL, 'ACTIVE', NOW());
SET @collection1 = LAST_INSERT_ID();

INSERT INTO collection (name, collection_type, discount_rate, description, start_date, end_date, status, created_at)
VALUES ('API연결테스트 이벤트 컬렉션', 'EVENT', 20.00, 'API 연결 테스트용 이벤트 컬렉션입니다.', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', NOW());
SET @collection2 = LAST_INSERT_ID();

INSERT INTO collection_book (collection_id, book_id, display_order, created_at) VALUES
  (@collection1, @book1, 1, NOW()),
  (@collection1, @book4, 2, NOW()),
  (@collection1, @book5, 3, NOW());

INSERT INTO collection_book (collection_id, book_id, display_order, created_at) VALUES
  (@collection2, @book2, NULL, NOW()),
  (@collection2, @book3, NULL, NOW());

-- ============================================================
-- §17. 원복(ROLLBACK) — 선택 실행. 위 §1~§16에서 만든 행만 자연키 기준으로 삭제한다.
--   세션 변수(@book1 등)에 의존하지 않으므로 나중에 새 연결/새 세션에서 그대로 실행해도 된다.
--   운영/공유 DB에서 실행하기 전에는 반드시 §0을 다시 실행해 대상行이 맞는지 확인한다.
-- ============================================================
-- DELETE FROM point_history WHERE member_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%');
-- DELETE FROM sanction_history WHERE member_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%');
-- DELETE FROM review_report WHERE reporter_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%')
--    OR review_id IN (SELECT review_id FROM review WHERE member_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%'));
-- DELETE FROM book_report WHERE reporter_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%');
-- DELETE FROM review WHERE member_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%');
-- DELETE FROM payment WHERE member_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%');
-- DELETE FROM book_rental WHERE member_id IN (SELECT member_id FROM member WHERE login_id LIKE 'apitest_%');
-- DELETE FROM collection_book WHERE collection_id IN (SELECT collection_id FROM collection WHERE name LIKE 'API연결테스트%');
-- DELETE FROM collection WHERE name LIKE 'API연결테스트%';
-- DELETE FROM book_detail WHERE book_id IN (SELECT book_id FROM book WHERE isbn LIKE '99900000%');
-- DELETE FROM book_category WHERE book_id IN (SELECT book_id FROM book WHERE isbn LIKE '99900000%');
-- DELETE FROM book_author WHERE book_id IN (SELECT book_id FROM book WHERE isbn LIKE '99900000%');
-- DELETE FROM book WHERE isbn LIKE '99900000%';
-- DELETE FROM member WHERE login_id LIKE 'apitest_%';
-- DELETE FROM category WHERE category_name LIKE 'API연결테스트%';
-- DELETE FROM author WHERE author_name LIKE 'API연결테스트%';
-- DELETE FROM publisher WHERE publisher_name = 'API연결테스트 출판사';
