API: API-AUTH-006 GET /api/v1/auth/check-login-id
목적: 회원가입 전 로그인 아이디 형식과 중복 여부 확인
흐름: 공통 @ValidLoginId 검증 -> existsByLoginId 조회 -> available 반환
DB: member.login_id 조회만 수행, 변경 없음
주의: 아이디 예약 기능이 아니며(확인 직후 다른 요청이 먼저 가입 가능), 운영 공개 전 Edge/Gateway 경로별 속도 제한이 별도 배포 조건
