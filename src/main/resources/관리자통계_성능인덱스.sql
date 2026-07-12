-- =====================================================================
-- 관리자 통계 확장(PR #42) 성능 개선용 인덱스 — 수동 실행 스크립트
-- 작성일: 2026-07-12
-- 관련 계획: docs/작업현황/2026-07-12_PR42_코드리뷰_대응계획.md 항목 3
--
-- 주의: 이 파일은 schema.sql/data.sql과 달리 spring.sql.init 설정(schema-locations=
-- classpath:schema.sql, data-locations=classpath:data.sql)에 포함돼 있지 않아
-- 서버 부팅 시 자동으로 실행되지 않는다. 각자 로컬/공유 MySQL DB에 필요할 때
-- 수동으로 실행한다(팀원마다 개별 DB를 쓰고 있어 schema.sql을 직접 고치면
-- 누가 어떤 인덱스를 실제로 적용했는지 추적하기 어렵기 때문에 분리했다).
--
-- 대상: 관리자 통계 확장(기간/나이대별 KPI, 대여·완독·결제 추이)에서 새로 조건절에
-- 쓰이는 컬럼 중 인덱스가 없던 3개. payment.paid_at/book_report.created_at/
-- review_report.created_at은 이미 인덱스가 있어 이 스크립트 대상에서 제외했다.
-- =====================================================================

CREATE INDEX idx_book_rental_created_at ON book_rental (created_at);
CREATE INDEX idx_reading_progress_completed_at ON reading_progress (completed_at);

-- review.status에는 이미 idx_review_status(단독) 인덱스가 있다. 관리자 통계는
-- status='ACTIVE' + created_at 범위를 함께 조건으로 쓰므로 복합 인덱스를 별도로 추가한다.
-- (기존 idx_review_status와 중복 여부/교체 필요성은 실제 데이터 규모 기준으로 후속 판단)
CREATE INDEX idx_review_status_created_at ON review (status, created_at);
