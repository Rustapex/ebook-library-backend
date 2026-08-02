-- member_grade seed 데이터
-- grade_id=1(씨앗)은 회원가입 기본 등급으로 코드에서 직접 참조하므로 반드시 씨앗이어야 한다.
-- 중복 실행 방지: ON DUPLICATE KEY UPDATE
INSERT INTO member_grade
    (grade_id, grade_name, grade_level, min_payment_amount, grade_period_months, maintain_payment_rate, description)
VALUES
    (1, '씨앗',   1,      0, 0, 10.00, '기본 등급. 가입 즉시 부여되며 유지기간 없음.'),
    (2, '새싹',   2,  20000, 1, 10.00, '1개월 누적 결제 2만 원 이상 달성 시 승급.'),
    (3, '나무',   3, 100000, 3, 10.00, '3개월 누적 결제 10만 원 이상 달성 시 승급.'),
    (4, '도토리', 4, 700000, 6, 10.00, '6개월 누적 결제 70만 원 이상 달성 시 승급.')
ON DUPLICATE KEY UPDATE
    grade_name            = VALUES(grade_name),
    grade_level           = VALUES(grade_level),
    min_payment_amount    = VALUES(min_payment_amount),
    grade_period_months   = VALUES(grade_period_months),
    maintain_payment_rate = VALUES(maintain_payment_rate),
    description           = VALUES(description);
