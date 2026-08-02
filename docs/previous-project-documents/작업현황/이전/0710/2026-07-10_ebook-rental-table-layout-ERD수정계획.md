# ebook-rental-table-layout-final.xlsx ERD 수정 계획

> 이 문서는 멘토 피드백 반영용 스프레드시트를 만들기 위한 사전 계획이다.
> 아직 실제 xlsx는 수정하거나 생성하지 않는다.
> 실제 Java/JPA/DB/API와 다른 검토용 설계가 포함될 수 있다.
> 실제 구현과 다른 항목은 숨기지 않고 영향도와 충돌 사항으로 기록한다.
> 계획 검토가 끝난 뒤 추가 수정 요청을 받고, 별도 요청에서 xlsx를 생성한다.
> 현재 계획에 적힌 명칭과 자료형은 검토 대상이며, 이번 문서 검토가 끝나기 전까지 실제 산출물에 적용하지 않는다.

## 5.1 작업 목적

- 이번 단계는 `2026-07-10_ebook-rental-table-layout-ERD수정계획.md` 계획 문서만 수정하는 단계다.
- 실제 스프레드시트는 아직 생성하지 않는다.
- 실제 코드, DB, DDL, DML, API 명세, 화면설계 문서는 변경하지 않는다.
- 목표는 멘토 피드백과 조사 결과를 반영해 다음 단계의 xlsx 생성 기준을 확정하는 것이다.

## 5.2 확인한 자료

| 구분 | 확인 파일/자료 | 확인 범위 | 확인 결과 |
|---|---|---|---|
| txt | `docs/작업현황/erd 수정.txt` | 전체 41줄 | 전체 대문자, 정수형 PK 제외, 회원/비밀번호/등급 피드백 확인 |
| txt | `docs/작업현황/0704 멘토링.txt` | 전체 | 컬럼명 축약, 회원 물리 삭제 의견, 쿠폰 만료 의견, 화면/API 정책 메모 확인 |
| 기존 계획 | `docs/작업현황/2026-07-10_ebook-rental-table-layout-ERD수정계획.md` | 전체 | 기존 계획을 본 문서로 전면 재작성 |
| 원본 xlsx | `docs/데이터베이스/table-layout/ebook-rental-table-layout-final.xlsx` | workbook.xml, 42개 worksheet 컬럼 행 | 42개 시트, 42개 테이블, 318개 컬럼 확인 |
| DDL | `src/main/resources/schema.sql` | CREATE TABLE, ALTER TABLE, CREATE INDEX, 회원/비밀번호/인덱스 구간 | 실제 실행 DDL은 43개 테이블이며 `book_isbn_temp` 임시 테이블 포함 |
| DDL | `docs/데이터베이스/ddl/schema.sql` | CREATE TABLE/CHECK/INDEX 검색 | 문서 DDL도 정수형 PK, 소문자 식별자, 상태 컬럼 혼재 확인 |
| DML | `src/main/resources/data.sql` | 전체 | `member_grade` seed가 `grade_level`, `description` 직접 사용 |
| API | `docs/API 명세/api-spec(final).md` | 인증, 회원, 쿠폰, 관리자, 제외 기능, enum 구간 | `loginId`, `role`, `status`, 비회원 비밀번호 재설정 제외, 물리 삭제 없음 확인 |
| 화면설계 | `docs/화면흐름도/01-공통-화면설계기준.md` | 인증/회원/상태/쿠폰/리뷰 정책 | 회원 탈퇴는 `WITHDRAWN` 상태, 물리 삭제하지 않음 |
| 화면설계 | `docs/화면흐름도/02-User-상세화면설계도.md` | U-002, U-003, U-004, U-005, U-015, U-017 | 회원가입/로그인/아이디 찾기에서 아이디 사용 |
| 화면설계 | `docs/화면흐름도/03-Admin-상세화면설계도.md` | A-002, A-003, A-013 | 관리자 회원 검색에 아이디/이름/이메일 사용 |
| 화면설계 | `docs/화면흐름도/04-전체-화면이동흐름.md` | 로그인, 탈퇴, 쿠폰, 알림, 상태 흐름 | 비회원 비밀번호 재설정 제외, 쿠폰 만료 표시, 탈퇴/비활성 로그인 거부 |
| 백엔드 규칙 | `docs/공통문서/백엔드_공통_개발_규칙.md` | DTO, Entity, BaseEntity, API 응답 규칙 | 실제 구현 변경 시 DTO/API/Entity 전반 영향 |
| PDF | `도서관정보나루_API_Manual.pdf` | 프로젝트 내 `*.pdf` 검색 | 현재 프로젝트 루트 안에서는 발견되지 않음. 다음 검토 시 파일 제공 필요 |
| 코드 검색 | `src/main/java`, `src/test/java` | `Long`, `GenerationType.IDENTITY`, `loginId`, `findByLoginId` 매칭 라인 | 실제 구현은 Long PK와 아이디 로그인 중심 |

## 5.3 피드백 원문 요약

| 피드백 항목 | 원문 요지 | 계획 반영 방향 |
|---|---|---|
| 전체 대문자 | 전체 대문자로 | 테이블명, 컬럼명, 제약조건명, 인덱스명을 `UPPER_SNAKE_CASE`로 계획 |
| 정수형 PK 제외 | PK는 int 자료형으로 하지 말 것 | `INT`, `BIGINT`, `UNSIGNED`, `AUTO_INCREMENT` PK/FK 제거 계획 |
| 회원 식별자 단일화 | `member_id` / `login_id` 둘 중 하나만 필요 | 내부 식별자는 `MEMBER_ID`만 유지, `LOGIN_ID` 삭제 계획 |
| 회원 비밀번호 명칭 | 암호화 비밀번호가 아니라 회원 비밀번호 | 스프레드시트 논리명은 `회원 비밀번호`, 물리명은 `MEMBER_PW` 계획 |
| 회원명 | 이름 -> `member_NM` | `MEMBER_NM` 계획 |
| 이메일 | 이메일 그대로 | `EMAIL` 계획, 일반 로그인 식별자로 사용 |
| 전화번호 | `phone -> member_`, 회원 전화번호 | `MEMBER_TEL_NO` 계획 |
| 생년월일 | `birth_date -> birth_DT` | `BIRTH_DT` 계획 |
| 성별 | gender는 모르겠음 | `GENDER_CD` 후보, 허용값은 미확정 |
| 관리자 여부 | role은 관리자 여부라서 role이 아님 | `ADMIN_YN` 계획, 기존 `USER/ADMIN` role과 충돌 기록 |
| 회원 상태 | status -> member_status | 프로젝트 표준 suffix에 맞춰 `MEMBER_ST` 계획 |
| 비식별화 일시 | `anonymized_at`이 무엇인지 확인 필요 | `ANONYMIZED_DTTM` 유지 계획, 정책 충돌 기록 |
| 비밀번호 재설정 코드 컬럼 순서 | `member_id`를 8번 뒤로 이동 | `MEMBER_ID`를 인증 정보 뒤로 이동 계획 |
| 인증코드 상태 명칭 | 인증코드 상태면 status가 아님 | `RESET_CODE_ST` 계획 |
| 회원 등급 순서 명칭 | `grade_level -> garde_ord` | 오타로 보고 `GRADE_ORD` 계획 |

## 5.4 조사 결과와 설계 결정

### 공식 자료 조사 요약

| 출처 | 확인한 핵심 내용 | 이번 계획에 반영할 결정 |
|---|---|---|
| RFC 9562 UUID, https://www.rfc-editor.org/rfc/rfc9562.html | UUID는 128비트 식별자이며 여러 버전을 정의한다. | 정수형 PK 제외 요구를 만족하는 후보로 UUID 사용 |
| Jakarta Persistence `GenerationType.UUID`, https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/generationtype | Jakarta Persistence 3.1은 UUID 식별자 생성 전략을 제공한다. | JPA에서 UUID 식별자 사용 가능하다는 근거로 기록 |
| Hibernate ORM User Guide, https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html | Hibernate는 UUID 타입과 식별자 생성을 지원한다. | 실제 구현 전환 시 Hibernate/JPA 매핑 변경 필요 |
| MySQL `CHAR`, `VARCHAR`, `BINARY`, https://dev.mysql.com/doc/refman/8.4/en/char.html | 문자열/바이너리 저장 타입의 길이와 저장 특성이 다르다. | 검토용 xlsx는 가독성을 위해 `CHAR(36)`, 운영 대안은 `BINARY(16)` 기록 |
| MySQL `UUID_TO_BIN`, `BIN_TO_UUID`, https://dev.mysql.com/doc/refman/8.4/en/miscellaneous-functions.html | UUID 문자열과 바이너리 표현 변환 함수가 있다. | `BINARY(16)`은 효율 대안으로만 기록하고 이번 계획은 `CHAR(36)` 유지 |
| MySQL InnoDB indexes, https://dev.mysql.com/doc/refman/8.4/en/innodb-index-types.html | InnoDB는 clustered index를 사용하고 secondary index가 PK 값을 포함한다. | UUID PK는 인덱스 크기/지역성 단점이 있음을 기록 |
| RFC 5321, https://www.rfc-editor.org/rfc/rfc5321.html | 이메일 경로 길이 제한은 최대 256 octets이며 실무에서 주소는 254자를 사용한다. | `EMAIL VARCHAR(254)` 계획, `VARCHAR(255)`도 흔함을 기록 |
| Google OpenID Connect, https://developers.google.com/identity/openid-connect/openid-connect | Google 계정의 안정 식별자는 이메일이 아니라 `sub` 클레임이다. | Google 로그인은 `EMAIL`이 아닌 `sub` 연결 구조가 필요 |
| OWASP Password Storage Cheat Sheet, https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html | 비밀번호는 원문 저장 금지, 안전한 단방향 해시와 salt/work factor 사용 필요. | 스프레드시트 논리명은 회원 비밀번호, 실제 저장은 해시여야 함 |
| 공공기관 데이터베이스 표준화 지침 | 이번 조사에서 공식 원문을 안정적으로 확인하지 못함. | 임의 약어 방지를 위해 프로젝트 내부 표준단어 사전으로 관리 |
| MySQL DATE/DATETIME/DECIMAL/정수형, https://dev.mysql.com/doc/refman/8.4/en/date-and-time-types.html, https://dev.mysql.com/doc/refman/8.4/en/fixed-point-types.html, https://dev.mysql.com/doc/refman/8.4/en/integer-types.html | 날짜, 날짜시간, 고정소수점, 정수형의 용도가 구분된다. | `_DT`는 `DATE`, `_DTTM`은 `DATETIME`, 금액/건수는 업무 범위에 맞춰 선택 |
| ISO ISBN, https://www.iso.org/standard/65483.html | ISBN은 국제 표준 식별자이며 ISBN-13은 13자리 체계다. | 내부 DB ISBN은 산술 대상이 아니므로 `CHAR(13)` 유지 |

### 결정별 상세

| 결정 | 피드백 내용 | 조사 결과 | 이번 계획에서 선택한 방식 | 선택 이유 | 단점 | 대안 | 실제 구현과의 차이 | 실제 적용 시 영향 |
|---|---|---|---|---|---|---|---|---|
| PK/FK UUID | 정수형 PK 제외 | JPA/Hibernate UUID 지원, MySQL은 CHAR/BINARY 모두 가능 | PK/FK `CHAR(36)` UUID | 멘토 피드백과 xlsx 가독성 우선 | 저장공간/인덱스 증가 | `BINARY(16)`, ULID, BIGINT 유지 | 현재 DDL/JPA는 `BIGINT UNSIGNED`/`Long`/IDENTITY | 모든 Entity, DTO, Repository, API path/id 타입, 테스트, seed 변경 필요 |
| `LOGIN_ID` 삭제 | 회원 식별자 단일화 | API/화면/코드는 `loginId` 사용 | `MEMBER_ID`만 유지, 일반 로그인은 `EMAIL` 사용 | 식별 컬럼 단일화 | 아이디 찾기 기능과 충돌 | `LOGIN_ID` 유지, 별도 auth 테이블 | 현재 회원가입/로그인/아이디 찾기 모두 `loginId` | 인증 API, 화면, Repository, 테스트 전면 영향 |
| 비밀번호 명칭 | 암호화 비밀번호 표현 수정 | OWASP는 원문 저장 금지와 해시 저장 권장 | `MEMBER_PW`, 논리명 `회원 비밀번호` | 멘토 피드백 반영, 암호화 오해 제거 | 원문 저장으로 오해 가능 | `PASSWORD_HASH` 유지하고 논리명만 변경 | 현재 코드/DDL은 `password_hash` | 실제 적용 시 컬럼명과 코드 변경 필요 |
| 이메일 | 이메일 그대로 | RFC 5321 근거상 254자 제한 사용 가능 | `EMAIL VARCHAR(254) UNIQUE` | 일반 로그인 식별자로 사용 | 기존 `VARCHAR(255)`와 차이 | `MEMBER_EMAIL VARCHAR(255)` 유지 | 현재 xlsx는 `member_email`, API는 `email` | DDL/API/화면/검증 영향 |
| 상태 suffix | status가 모호함 | 표준 약어는 내부 정의 필요 | 상태 컬럼은 `_ST` | 멘토의 축약 요구 반영 | 외부 표준 강제 약어 아님 | `_STATUS`, `_CD` | 현재 `status`, `member_status`, `book_status` 혼재 | CHECK, INDEX, Query, DTO 영향 |
| 날짜 suffix | `birth_date -> birth_DT` | MySQL `DATE`와 `DATETIME` 구분 | 날짜 `_DT`, 날짜시간 `_DTTM` | 의미와 타입 일치 | 현재 `_AT` 관례와 충돌 | `_DATE`, `_AT` 유지 | 현재 DDL/API/Java는 `createdAt`, `birthDate` | 컬럼명, API 필드명 변환 규칙 필요 |
| 관리자 여부 | role은 관리자 여부가 아님 | API/화면은 `USER/ADMIN` 역할 분기 사용 | `ADMIN_YN CHAR(1)` | 피드백 직접 반영 | 다중 역할 확장성 낮음 | `ROLE_CD`, 권한 테이블 | 현재 Spring Security는 role 사용 | 인증/인가 로직 전면 영향 |
| 비식별화 | 의미 확인 필요 | final API/화면은 물리 삭제 없음, 0704 메모는 한 달 후 물리 삭제 | `ANONYMIZED_DTTM` 유지 | final 문서 기준 우선 | 멘토 물리 삭제 의견과 충돌 | 컬럼 삭제, 물리 삭제 배치 | 현재 xlsx에만 명확, 구현은 제한적 | 개인정보 정책 결정 필요 |
| ISBN | 식별자 성격 | ISBN-13은 13자리 식별자 | `ISBN CHAR(13) UNIQUE` | 앞자리 0 보존, 산술 대상 아님 | ISBN-10/하이픈 입력 별도 처리 필요 | `VARCHAR(17)` | 현재 `book_isbn CHAR(13)` | 명칭만 `ISBN`으로 단순화 가능 |

## 5.5 표준단어 및 약어 사전

| suffix | 의미 | 예시 |
|---|---|---|
| `_ID` | 식별자 | `MEMBER_ID` |
| `_NM` | 명칭 | `MEMBER_NM` |
| `_PW` | 비밀번호 | `MEMBER_PW` |
| `_CD` | 코드 | `GENDER_CD` |
| `_ST` | 상태 | `MEMBER_ST` |
| `_TP` | 유형 | `PAYMENT_TP` |
| `_DT` | 날짜 | `BIRTH_DT` |
| `_DTTM` | 날짜시간 | `CREATED_DTTM` |
| `_YN` | 여부 | `ADMIN_YN` |
| `_ORD` | 순서 | `GRADE_ORD` |
| `_CNT` | 건수 | `RENTAL_CNT` |
| `_AMT` | 금액 | `PAYMENT_AMT` |
| `_RATE` | 비율 | `MAINTAIN_PAYMENT_RATE` |
| `_NO` | 번호 | `MEMBER_TEL_NO` |
| `_URL` | URL | `COVER_IMAGE_URL` |

추가 규칙:

- 모든 테이블명과 컬럼명은 `UPPER_SNAKE_CASE`로 계획한다.
- 약어 사전에 없는 임의 약어는 사용하지 않는다.
- `EMAIL`, `ISBN`, `URL`처럼 그 자체로 의미가 명확한 표준 단어는 불필요하게 테이블 접두어를 붙이지 않는다.
- 모든 컬럼 앞에 테이블명을 기계적으로 반복하지 않는다.
- 단독으로 모호한 이름만 업무 단어를 붙인다.
- `_ST`는 MySQL이나 국제 표준이 강제하는 약어가 아니라 멘토의 컬럼 축약 요구를 반영한 프로젝트 내부 표준이다.
- 상태 컬럼에서는 `_ST`와 `_CD`를 혼용하지 않는다.

## 5.6 자료형 및 도메인 규칙

| 도메인 | 계획 자료형 | 규칙 |
|---|---|---|
| UUID PK/FK | `CHAR(36)` | 하이픈 포함 UUID 문자열, `NOT NULL`, PK/FK 동일 타입 |
| 이메일 | `VARCHAR(254)` | `EMAIL`, `NOT NULL`, `UNIQUE`, 애플리케이션 이메일 검증 병행 |
| 비밀번호 | `VARCHAR(255)` | 스프레드시트 논리명은 회원 비밀번호, 실제 저장값은 단방향 해시 |
| 회원명 | `VARCHAR(50)` | `MEMBER_NM` |
| 전화번호 | `VARCHAR(20)` | `MEMBER_TEL_NO`, 형식 검증은 애플리케이션 |
| URL | `VARCHAR(500)` | `COVER_IMAGE_URL` 등 |
| ISBN | `CHAR(13)` | 하이픈 없는 ISBN-13, UNIQUE |
| 날짜 | `DATE` | `_DT` |
| 날짜시간 | `DATETIME` | `_DTTM` |
| 여부 | `CHAR(1)` | `Y/N CHECK` |
| 상태/유형 | `VARCHAR(n)` | 허용값 최대 길이에 맞춤, CHECK 유지 |
| 순서/건수 | `INT` | PK/FK에는 사용 금지 |
| 별점 | `TINYINT` | 1~5 CHECK |
| 평균 별점 | `DECIMAL(3,2)` | 예: 4.50 |
| 비율 | `DECIMAL(5,2)` | 예: 10.00 |
| 일반 원화 금액 | `INT` | 서비스 범위 내 금액 |
| 큰 누적 금액 | 필요 시 `BIGINT` | PK/FK가 아닌 누적 집계에는 사용 가능 |
| 일반 설명 | `TEXT` | 소개/설명/사유 |
| 전자책 페이지 본문 | `LONGTEXT` | 페이지 본문 |

PK/FK에서는 `INT`, `BIGINT`, `INT UNSIGNED`, `BIGINT UNSIGNED`를 사용하지 않는다. 다만 PK가 아닌 큰 누적 금액이나 집계 건수에는 업무 범위 근거가 있으면 `BIGINT`를 사용할 수 있다.

## 5.7 회원 테이블 수정 계획

향후 검토용 스프레드시트의 회원 시트는 다음 순서로 계획한다. `LOGIN_ID`는 삭제 예정이며 순번은 다시 매긴다.

| 순번 | 변경 예정 컬럼명 | 논리명 | 타입 | 제약조건 계획 | 설명 |
|---:|---|---|---|---|---|
| 1 | `MEMBER_ID` | 회원 식별자 | `CHAR(36)` | PK, NOT NULL | 시스템 내부 UUID 식별자. 사용자가 로그인할 때 직접 입력하지 않음 |
| 2 | `MEMBER_PW` | 회원 비밀번호 | `VARCHAR(255)` | NOT NULL | 스프레드시트 논리명. 실제 저장값은 안전한 단방향 해시 |
| 3 | `MEMBER_NM` | 회원명 | `VARCHAR(50)` | NOT NULL | 기존 `member_name`/`name` 통합 |
| 4 | `EMAIL` | 이메일 | `VARCHAR(254)` | NOT NULL, UNIQUE | 일반 로그인 식별자. 원문 보존 기준이며 정규화는 실제 컬럼 없이 비고/검증 정책으로 관리 |
| 5 | `MEMBER_TEL_NO` | 회원 전화번호 | `VARCHAR(20)` | NULL | 기존 `phone` 변경 |
| 6 | `BIRTH_DT` | 생년월일 | `DATE` | NOT NULL | 기존 `birth_date` 변경 |
| 7 | `GENDER_CD` | 성별 코드 | `CHAR(1)` | NULL, CHECK `GENDER_CD IN ('M', 'F')` | 비필수 입력. 허용값은 `M`, `F`로 제한 |
| 8 | `ADMIN_YN` | 관리자 여부 | `CHAR(1)` | NOT NULL, DEFAULT `N`, CHECK `Y/N` | 기존 `role` 대체 후보 |
| 9 | `GOOGLE_SUB` | Google 계정 식별자 | `VARCHAR(255)` | NULL, UNIQUE | Google 로그인 연결 시 `sub` 저장. 별도 인증 테이블과 추가 로그인 제공자 컬럼은 만들지 않음 |
| 10 | `GRADE_ID` | 회원 등급 식별자 | `CHAR(36)` | FK, NOT NULL | `MEMBER_GRADE.GRADE_ID` 참조 |
| 11 | `GRADE_EXPIRE_DTTM` | 등급 유지 종료 일시 | `DATETIME` | NULL | 기존 `grade_expires_at` 변경 |
| 12 | `GRADE_PAYMENT_START_DTTM` | 등급 산정 시작 일시 | `DATETIME` | NULL | 기존 `grade_payment_started_at` 변경 |
| 13 | `POINT_BALANCE` | 포인트 잔액 | `INT` | NOT NULL, DEFAULT 0, CHECK `>= 0` | 기존 데이터 범위 검토 |
| 14 | `MEMBER_ST` | 회원 상태 | `VARCHAR(20)` | NOT NULL, CHECK 기존 허용값 유지 | `JOINED`, `SANCTIONED`, `WITHDRAWN`, `DEACTIVATED` |
| 15 | `WITHDRAWN_DTTM` | 회원 탈퇴 일시 | `DATETIME` | NULL | 기존 `withdrawn_at` 변경 |
| 16 | `DEACTIVATED_DTTM` | 관리자 비활성 처리 일시 | `DATETIME` | NULL | 기존 `deactivated_at` 변경 |
| 17 | `ANONYMIZED_DTTM` | 개인정보 비식별화 일시 | `DATETIME` | NULL | 회원 개인정보를 비식별 값으로 변경한 일시 |
| 18 | `WITHDRAWAL_REASON` | 탈퇴/비활성 사유 | `VARCHAR(255)` | NULL | 명칭은 추가 검토 |
| 19 | `CREATED_DTTM` | 생성 일시 | `DATETIME` | NOT NULL | 기존 `created_at` 변경 |
| 20 | `UPDATED_DTTM` | 수정 일시 | `DATETIME` | NULL | 기존 `updated_at` 변경 |

`LOGIN_ID` 삭제 계획:

- 향후 검토용 스프레드시트에서 삭제한다.
- 삭제 이유는 멘토 피드백에 따른 회원 식별 컬럼 단일화다.
- 기존 API와 코드에서 `loginId`, `findByLoginId`, 아이디 찾기를 사용한다는 점은 영향도에 기록한다.
- 일반 로그인 식별자는 `EMAIL NOT NULL UNIQUE`로 계획한다.

Google 로그인 계획:

- 별도 Google 로그인 인증 테이블은 만들지 않고 `MEMBER.GOOGLE_SUB` 컬럼을 추가하는 방향으로 계획한다.
- Google 계정 식별은 이메일이 아니라 Google `sub`를 사용해야 한다.
- `GOOGLE_SUB`는 NULL 허용, 값이 있을 때 UNIQUE로 계획한다.
- `GOOGLE_LINKED_DTTM`, `LOGIN_PROVIDER_CD`는 이번 계획에서 추가하지 않는다.
- 추가 컬럼을 두지 않는 이유는 회원 테이블 속성 증가를 줄이고, 현재 범위가 Google 단일 연동 최소 반영안이기 때문이다. 여러 소셜 제공자를 확장할 경우에는 회원 테이블 컬럼 추가보다 별도 인증 구조가 더 자연스럽다.

회원 비밀번호 계획:

- 스프레드시트 표현은 `MEMBER_PW`, 논리명 `회원 비밀번호`, 설명 `회원 비밀번호`로 계획한다.
- `암호화 비밀번호`, `회원 비밀번호 해시`, `비밀번호 암호문` 표현은 스프레드시트에서 사용하지 않는다.
- 단, 실제 구현에서는 원문 비밀번호를 저장하면 안 되며 안전한 단방향 비밀번호 해시를 저장해야 한다.
- 기존 `암호화 비밀번호`는 복호화 가능한 암호화로 오해될 수 있으므로 피한다.

개인정보 비식별화 계획:

- 기존 `ANONYMIZED_AT`은 `ANONYMIZED_DTTM`으로 변경 계획한다.
- final API/화면 정책은 `WITHDRAWN` 상태 유지 및 물리 삭제하지 않음이다.
- 0704 멘토링 메모에는 한 달 이후 물리 삭제 의견이 있어 정책 충돌로 기록한다.
- 이번 계획에서는 final 문서를 기준으로 컬럼 유지 방향을 적되, 실제 xlsx에는 아직 반영하지 않는다.

## 5.8 비밀번호 재설정 코드 수정 계획

final API에서는 비회원 비밀번호 재설정 기능이 제외됐지만, 피드백 대상이므로 시트를 삭제하지 않는다. 비고에 final API 범위 제외 기능이라고 표시한다.

| 순번 | 변경 예정 컬럼명 | 논리명 | 타입 | 제약조건 계획 | 설명 |
|---:|---|---|---|---|---|
| 1 | `PASSWORD_RESET_CODE_ID` | 비밀번호 재설정 인증 코드 식별자 | `CHAR(36)` | PK, NOT NULL | `AUTO_INCREMENT`, `UNSIGNED` 제거 |
| 2 | `EMAIL` | 인증 코드 발송 이메일 | `VARCHAR(254)` | NOT NULL | 기존 `reset_email` 또는 `email` 통합 |
| 3 | `CODE_HASH` | 인증 코드 저장값 | `VARCHAR(255)` | NOT NULL | 실제 원문 저장 금지 |
| 4 | `RESET_CODE_ST` | 인증코드 상태 | `VARCHAR(20)` | NOT NULL, CHECK 기존 허용값 | `ISSUED`, `VERIFIED`, `USED`, `EXPIRED`, `FAILED` 유지 |
| 5 | `FAIL_CNT` | 인증 실패 횟수 | `INT` | NOT NULL, DEFAULT 0 | 기존 `fail_count` 변경 |
| 6 | `EXPIRE_DTTM` | 인증 코드 만료 일시 | `DATETIME` | NOT NULL | 기존 `expires_at` 변경 |
| 7 | `VERIFIED_DTTM` | 인증 성공 일시 | `DATETIME` | NULL | 기존 `verified_at` 변경 |
| 8 | `MEMBER_ID` | 재설정 대상 회원 식별자 | `CHAR(36)` | FK, NOT NULL | 피드백에 따라 인증 정보 뒤로 이동 |
| 9 | `USED_DTTM` | 비밀번호 재설정 완료 일시 | `DATETIME` | NULL | 기존 `used_at` 변경 |
| 10 | `CREATED_DTTM` | 생성 일시 | `DATETIME` | NOT NULL | 기존 `created_at` 변경 |
| 11 | `UPDATED_DTTM` | 수정 일시 | `DATETIME` | NULL | 기존 `updated_at` 변경 |

## 5.9 회원 등급 수정 계획

| 변경 전 | 변경 예정 | 논리명 | 타입 | 제약조건 계획 | 영향 |
|---|---|---|---|---|---|
| `grade_id` | `GRADE_ID` | 회원 등급 식별자 | `CHAR(36)` | PK, NOT NULL | DDL/JPA/DML의 `grade_id` 변경 영향 |
| `grade_name` | `GRADE_NM` | 등급명 | `VARCHAR(50)` | UNIQUE, NOT NULL | `data.sql` seed 수정 영향 |
| `grade_level` | `GRADE_ORD` | 등급 순서 | `INT` | UNIQUE, NOT NULL | `data.sql`, `MemberGrade.gradeLevel` 변경 영향 |
| `min_payment_amount` | `MIN_PAYMENT_AMT` | 승급 필요 결제 금액 | `INT` | NOT NULL, DEFAULT 0 | DML/코드 영향 |
| `grade_period_months` | `GRADE_PERIOD_MONTH_CNT` | 등급 유지 개월 수 | `INT` | NOT NULL | DML/코드 영향 |
| `maintain_payment_rate` | `MAINTAIN_PAYMENT_RATE` | 유지 조건 결제율 | `DECIMAL(5,2)` | NOT NULL | 유지 |
| `grade_description` 또는 `description` | `GRADE_DESC` | 등급 설명 | `TEXT` | NULL | xlsx와 DDL/DML 명칭 불일치 해소 |
| 없음 | `CREATED_DTTM`, `UPDATED_DTTM` | 생성/수정 일시 | `DATETIME` | 검토 | BaseEntity 규칙과 충돌 여부 확인 |

## 5.10 전체 테이블 공통 적용 규칙

- 단일 PK는 `CHAR(36) NOT NULL PRIMARY KEY`로 계획한다.
- `AUTO_INCREMENT`, `UNSIGNED`는 PK/FK에서 제거한다.
- UUID 생성 주체는 애플리케이션 또는 JPA로 둔다.
- UUID 값은 하이픈을 포함한 문자열로 스프레드시트에 표현한다.
- UUID PK를 참조하는 모든 FK도 `CHAR(36)`으로 맞춘다.
- N:M 매핑 테이블의 복합 PK 구조는 유지하되, 복합 PK를 구성하는 FK 타입은 `CHAR(36)`으로 변경한다.
- 사용하지 않을 PK/FK 타입은 `INT`, `BIGINT`, `INT UNSIGNED`, `BIGINT UNSIGNED`다.
- 상태 컬럼은 `_ST`, 유형 컬럼은 `_TP`, 날짜는 `_DT`, 날짜시간은 `_DTTM`, 순서는 `_ORD`, 건수는 `_CNT`, 금액은 `_AMT`, 여부는 `_YN`을 우선 적용한다.
- CHECK 내부 컬럼 참조와 enum 값은 변경된 컬럼명/대문자 값과 일치해야 한다.
- 제약조건/인덱스 접두어는 `PK_`, `UK_`, `FK_`, `CHK_`, `IX_`로 계획한다.
- 예: `PK_MEMBER`, `UK_MEMBER_EMAIL`, `FK_BOOK_RENTAL_MEMBER`, `CHK_MEMBER_ST`, `IX_BOOK_TITLE`.
- 컬럼명이 바뀌면 CHECK 내부 참조, 인덱스, FK, 초기값, 비고도 함께 변경한다.

UUID 선택 이유:

- 멘토의 정수형 PK 제외 요청 반영.
- JPA가 UUID 식별자를 지원함.
- 애플리케이션에서 ID를 독립적으로 생성 가능.
- 자동 증가 숫자에 의존하지 않음.
- 외부에서 단순 순번 추측이 어려움.
- 시스템 간 데이터 병합 시 충돌 가능성을 낮출 수 있음.

UUID 단점과 대안:

- `CHAR(36)`은 `BIGINT`보다 저장 공간을 많이 사용한다.
- 인덱스 크기가 커질 수 있다.
- 무작위 UUID는 B-tree 인덱스 지역성이 좋지 않을 수 있다.
- 실제 운영 DB에서는 `BINARY(16)`이 더 효율적일 수 있다.
- UUID 버전에 따라 정렬 및 삽입 특성이 달라질 수 있다.
- 이번 계획에서는 멘토 피드백과 스프레드시트 가독성을 우선하여 `CHAR(36)`을 선택한다.

## 5.11 전체 42개 테이블별 변경 계획

아래 표는 실제 xlsx를 수정하지 않고 다음 단계에서 반영할 계획만 정리한다. 변경하지 않는 컬럼은 유지 이유를 함께 적는다.

| 테이블 | 기존 컬럼명 | 변경 예정 컬럼명 | 변경 예정 논리명 | 변경 예정 타입 | 제약조건 계획 | 변경 이유 | 실제 적용 영향 |
|---|---|---|---|---|---|---|---|
| `member` | 전체 20개 컬럼 | `MEMBER_ID`, `MEMBER_PW`, `MEMBER_NM`, `EMAIL`, `MEMBER_TEL_NO`, `BIRTH_DT`, `GENDER_CD`, `ADMIN_YN`, `GOOGLE_SUB`, `GRADE_ID`, `GRADE_EXPIRE_DTTM`, `GRADE_PAYMENT_START_DTTM`, `POINT_BALANCE`, `MEMBER_ST`, `WITHDRAWN_DTTM`, `DEACTIVATED_DTTM`, `ANONYMIZED_DTTM`, `WITHDRAWAL_REASON`, `CREATED_DTTM`, `UPDATED_DTTM` | 회원 | PK/FK `CHAR(36)`, 이메일 `VARCHAR(254)`, YN `CHAR(1)` | PK/UK/FK/CHECK/INDEX 대문자화 | 직접 피드백, 로그인 식별자 단일화, 이메일 정규화는 비고 정책으로 관리, Google `sub` 컬럼 반영 | API/화면/인증/Repository/테스트 전면 영향 |
| `password_reset_code` | 전체 11개 컬럼 | `PASSWORD_RESET_CODE_ID`, `EMAIL`, `CODE_HASH`, `RESET_CODE_ST`, `FAIL_CNT`, `EXPIRE_DTTM`, `VERIFIED_DTTM`, `MEMBER_ID`, `USED_DTTM`, `CREATED_DTTM`, `UPDATED_DTTM` | 비밀번호 재설정 코드 | PK/FK `CHAR(36)`, 이메일 `VARCHAR(254)` | `CHK_PASSWORD_RESET_CODE_ST` | 직접 피드백, 컬럼 순서 변경 | final API 제외 기능이나 DDL/JPA 존재 시 영향 |
| `member_grade` | 전체 7개 컬럼 | `GRADE_ID`, `GRADE_NM`, `GRADE_ORD`, `MIN_PAYMENT_AMT`, `GRADE_PERIOD_MONTH_CNT`, `MAINTAIN_PAYMENT_RATE`, `GRADE_DESC` | 회원 등급 | PK `CHAR(36)` | `UK_MEMBER_GRADE_GRADE_ORD` | `grade_level` 순서화, 약어 적용 | `data.sql`, `MemberGrade` 영향 |
| `attendance` | `attendance_id`, `member_id`, `attendance_date`, `created_at` | `ATTENDANCE_ID`, `MEMBER_ID`, `ATTENDANCE_DT`, `CREATED_DTTM` | 출석 | PK/FK `CHAR(36)`, 날짜 `DATE` | 복합 UK 유지 | PK/FK UUID, 날짜 suffix | 출석 Repository/테스트 영향 |
| `point_history` | 전체 7개 컬럼 | `POINT_HISTORY_ID`, `MEMBER_ID`, `PAYMENT_ID`, `POINT_TP`, `POINT_AMT`, `POINT_DESC`, `CREATED_DTTM` | 포인트 이력 | PK/FK `CHAR(36)` | `POINT_TP` CHECK 대문자 값 | `challenge_reward` 소문자 enum 수정 | 포인트 API/테스트 영향 |
| `publisher` | 4개 컬럼 | `PUBLISHER_ID`, `PUBLISHER_NM`, `PUBLISHER_ST`, `CREATED_DTTM` | 출판사 | PK `CHAR(36)` | 상태 CHECK | `_NM`, `_ST`, `_DTTM` | 도서/출판사 코드 영향 |
| `author` | 5개 컬럼 | `AUTHOR_ID`, `AUTHOR_NM`, `AUTHOR_INTRO`, `AUTHOR_ST`, `CREATED_DTTM` | 저자 | PK `CHAR(36)` | 상태 CHECK | 명칭/상태 suffix | 도서 저자 매핑 영향 |
| `book` | 전체 15개 컬럼 | `BOOK_ID`, `PUBLISHER_ID`, `BOOK_TITLE`, `ISBN`, `COVER_IMAGE_URL`, `RENTAL_TP`, `RENTAL_PRICE_AMT`, `DEFAULT_RENTAL_DAY_CNT`, `TOTAL_PAGE_CNT`, `RENTAL_CNT`, `AVG_RATING`, `REVIEW_CNT`, `BOOK_ST`, `CREATED_DTTM`, `UPDATED_DTTM` | 도서 | PK/FK `CHAR(36)`, ISBN `CHAR(13)` | `UK_BOOK_ISBN`, 상태/유형 CHECK | ISBN 식별자, suffix 통일 | API bookId Long 및 도서 검색 영향 |
| `book_author` | `book_id`, `author_id`, `author_order` | `BOOK_ID`, `AUTHOR_ID`, `AUTHOR_ORD` | 도서 저자 | FK `CHAR(36)`, 순서 `INT` | 복합 PK 유지 | N:M 복합키 유지, 타입만 UUID | 복합키 Embeddable 영향 |
| `book_detail` | 8개 컬럼 | `BOOK_DETAIL_ID`, `BOOK_ID`, `BOOK_DESC`, `TABLE_OF_CONTENTS`, `PUBLISHER_REVIEW`, `DETAIL_CONTENT`, `CREATED_DTTM`, `UPDATED_DTTM` | 도서 상세 | PK/FK `CHAR(36)` | `UK_BOOK_DETAIL_BOOK` 유지 | 설명 suffix 정리 | 상세 API 영향 |
| `book_page` | 6개 컬럼 | `BOOK_PAGE_ID`, `BOOK_ID`, `PAGE_NO`, `PAGE_CONTENT`, `CREATED_DTTM`, `UPDATED_DTTM` | 도서 페이지 | PK/FK `CHAR(36)`, 본문 `LONGTEXT` | `(BOOK_ID, PAGE_NO)` UK 유지 | 페이지 번호는 `_NO` 유지 | 뷰어/페이지 API 영향 |
| `category` | 5개 컬럼 | `CATEGORY_ID`, `PARENT_CATEGORY_ID`, `CATEGORY_NM`, `DISPLAY_ORD`, `CATEGORY_ST` | 카테고리 | PK/FK `CHAR(36)` | 상태 CHECK, 상위+명칭 UK 검토 | 명칭/상태/순서 suffix | 카테고리 API 영향 |
| `book_category` | 2개 컬럼 | `BOOK_ID`, `CATEGORY_ID` | 도서 카테고리 | FK `CHAR(36)` | 복합 PK 유지 | N:M 구조 유지 | 복합키 영향 |
| `book_keyword` | 5개 컬럼 | `KEYWORD_ID`, `KEYWORD_NM`, `KEYWORD_TP`, `KEYWORD_ST`, `CREATED_DTTM` | 도서 키워드 | PK `CHAR(36)` | UK/CHECK 유지 | 유형/상태 suffix | 키워드 조회 영향 |
| `book_keyword_map` | 3개 컬럼 | `BOOK_ID`, `KEYWORD_ID`, `DISPLAY_ORD` | 도서 키워드 매핑 | FK `CHAR(36)` | 복합 PK 유지 | 순서 suffix | 복합키 영향 |
| `book_rental` | 7개 컬럼 | `RENTAL_ID`, `MEMBER_ID`, `BOOK_ID`, `RENTAL_START_DTTM`, `RENTAL_END_DTTM`, `RENTAL_ST`, `CREATED_DTTM` | 도서 대여 | PK/FK `CHAR(36)` | 상태 CHECK | 날짜시간/상태 suffix | 대여 API 영향 |
| `reading_progress` | 7개 컬럼 | `PROGRESS_ID`, `RENTAL_ID`, `CURRENT_PAGE_NO`, `READ_PAGE_CNT`, `LAST_READ_DTTM`, `COMPLETED_YN`, `COMPLETED_DTTM` | 독서 진행 | PK/FK `CHAR(36)`, YN `CHAR(1)` | `COMPLETED_YN` Y/N CHECK | TINYINT 여부를 Y/N으로 통일 | 뷰어 진행 저장 영향 |
| `bookmark` | 4개 컬럼 | `BOOKMARK_ID`, `RENTAL_ID`, `PAGE_NO`, `CREATED_DTTM` | 북마크 | PK/FK `CHAR(36)` | `(RENTAL_ID, PAGE_NO)` UK 유지 | 날짜시간 suffix | 북마크 API 영향 |
| `favorite_book` | 4개 컬럼 | `FAVORITE_ID`, `MEMBER_ID`, `BOOK_ID`, `CREATED_DTTM` | 즐겨찾기 도서 | PK/FK `CHAR(36)` | `(MEMBER_ID, BOOK_ID)` UK 유지 | PK/FK UUID | 즐겨찾기 API 영향 |
| `review` | 7개 컬럼 | `REVIEW_ID`, `MEMBER_ID`, `BOOK_ID`, `REVIEW_CONTENT`, `REVIEW_ST`, `CREATED_DTTM`, `UPDATED_DTTM` | 리뷰 | PK/FK `CHAR(36)` | 상태 CHECK | `_ST`, `_DTTM` | 리뷰 API 영향 |
| `rating` | 5개 컬럼 | `RATING_ID`, `MEMBER_ID`, `BOOK_ID`, `RATING_SCORE`, `CREATED_DTTM` | 별점 | PK/FK `CHAR(36)`, `TINYINT` | 1~5 CHECK | 별점 도메인 유지 | 별점 API/통계 영향 |
| `book_report` | 9개 컬럼 | `BOOK_REPORT_ID`, `REPORTER_ID`, `BOOK_ID`, `REPORT_TP`, `BOOK_REPORT_CONTENT`, `REPORT_ST`, `PROCESSED_DTTM`, `PROCESS_RESULT`, `CREATED_DTTM` | 도서 신고 | PK/FK `CHAR(36)` | 상태 CHECK | 유형/상태/일시 suffix | 신고 API 영향 |
| `review_report` | 9개 컬럼 | `REVIEW_REPORT_ID`, `REPORTER_ID`, `REVIEW_ID`, `REPORT_TP`, `REVIEW_REPORT_CONTENT`, `REPORT_ST`, `PROCESSED_DTTM`, `PROCESS_RESULT`, `CREATED_DTTM` | 리뷰 신고 | PK/FK `CHAR(36)` | 상태 CHECK | 유형/상태/일시 suffix | 신고 API 영향 |
| `sanction_history` | 8개 컬럼 | `SANCTION_HISTORY_ID`, `MEMBER_ID`, `REVIEW_REPORT_ID`, `SANCTION_TP`, `SANCTION_REASON`, `START_DTTM`, `END_DTTM`, `CREATED_DTTM` | 제재 이력 | PK/FK `CHAR(36)` | 유형 CHECK | `_TP`, `_DTTM` | 제재/관리자 API 영향 |
| `payment` | 10개 컬럼 | `PAYMENT_ID`, `MEMBER_ID`, `RENTAL_ID`, `PAYMENT_TP`, `ORIGINAL_AMT`, `PAYMENT_AMT`, `PAYMENT_METHOD`, `PAYMENT_ST`, `PAID_DTTM`, `CREATED_DTTM` | 결제 | PK/FK `CHAR(36)` | 금액 CHECK, 상태 CHECK | 금액/유형/상태 suffix | 결제 API 영향 |
| `coupon` | 7개 컬럼 | `COUPON_ID`, `COUPON_NM`, `COUPON_TP`, `BENEFIT_VALUE`, `BENEFIT_UNIT`, `COUPON_ST`, `CREATED_DTTM` | 쿠폰 | PK `CHAR(36)` | 상태/단위 CHECK | 쿠폰 만료 정책 제거, `VALID_DAY_CNT` 제외 | 쿠폰 API/화면 정책 영향 |
| `member_coupon` | 6개 컬럼 | `MEMBER_COUPON_ID`, `MEMBER_ID`, `COUPON_ID`, `COUPON_ST`, `ISSUED_DTTM`, `USED_DTTM` | 회원 쿠폰 | PK/FK `CHAR(36)` | 상태 CHECK | 쿠폰 만료 정책 제거, `EXPIRE_DTTM` 제외 | 쿠폰 목록/만료 표시 영향 |
| `coupon_usage` | 7개 컬럼 | `COUPON_USAGE_ID`, `MEMBER_COUPON_ID`, `RENTAL_ID`, `PAYMENT_ID`, `DISCOUNT_AMT`, `USAGE_NOTE`, `USED_DTTM` | 쿠폰 사용 | PK/FK `CHAR(36)` | `MEMBER_COUPON_ID`, `PAYMENT_ID` UK 유지 | 금액/일시 suffix | 결제 할인 영향 |
| `challenge` | 7개 컬럼 | `CHALLENGE_ID`, `CHALLENGE_NM`, `CHALLENGE_TP`, `TARGET_ACTION`, `TARGET_CNT`, `CHALLENGE_ST`, `CREATED_DTTM` | 챌린지 | PK `CHAR(36)` | 유형/상태 CHECK | `_NM`, `_TP`, `_CNT`, `_ST` | 챌린지 API 영향 |
| `challenge_progress` | 8개 컬럼 | `CHALLENGE_PROGRESS_ID`, `MEMBER_ID`, `CHALLENGE_ID`, `PROGRESS_DT`, `CURRENT_CNT`, `ACHIEVED_YN`, `ACHIEVED_DTTM`, `REWARD_CLAIMED_DTTM` | 챌린지 진행 | PK/FK `CHAR(36)`, YN `CHAR(1)` | 복합 UK 유지 | 날짜/건수/여부 suffix | 진행/보상 API 영향 |
| `challenge_reward` | 6개 컬럼 | `CHALLENGE_REWARD_ID`, `CHALLENGE_ID`, `REWARD_TP`, `REWARD_POINT`, `COUPON_ID`, `REWARD_QTY` | 챌린지 보상 | PK/FK `CHAR(36)` | `COUPON` enum 대문자 | 소문자 enum 수정 | 보상 지급 영향 |
| `book_request_candidate` | 7개 컬럼 | `REQUEST_CANDIDATE_ID`, `NORMALIZED_TITLE`, `NORMALIZED_AUTHOR`, `NORMALIZED_PUBLISHER`, `REQUEST_CNT`, `FIRST_REQUESTED_DTTM`, `CANDIDATE_ST` | 희망도서 후보 | PK `CHAR(36)` | 상태 CHECK | 건수/일시/상태 suffix | 희망도서 관리 영향 |
| `book_request` | 15개 컬럼 | `REQUEST_ID`, `REQUEST_CANDIDATE_ID`, `MEMBER_ID`, `APPROVED_BOOK_ID`, `REQUEST_TITLE`, `REQUEST_AUTHOR`, `REQUEST_PUBLISHER`, `NORMALIZED_TITLE`, `NORMALIZED_AUTHOR`, `NORMALIZED_PUBLISHER`, `REQUEST_REASON`, `REQUEST_ST`, `PROCESSED_DTTM`, `REJECT_REASON`, `CREATED_DTTM` | 희망도서 신청 | PK/FK `CHAR(36)` | 복합 UK/상태 CHECK 유지 | 상태/일시 suffix | 희망도서 API 영향 |
| `library` | 6개 컬럼 | `LIBRARY_ID`, `LIBRARY_NM`, `LIBRARY_ADDRESS`, `LIBRARY_LATITUDE`, `LIBRARY_LONGITUDE`, `LIBRARY_ST` | 도서관 | PK `CHAR(36)`, 좌표 `DECIMAL(10,7)` | 상태 CHECK | 명칭/상태 suffix | 도서관 API 영향 |
| `library_book` | 5개 컬럼 | `LIBRARY_BOOK_ID`, `LIBRARY_ID`, `BOOK_ID`, `HOLDING_ST`, `UPDATED_DTTM` | 도서관 도서 | PK/FK `CHAR(36)` | 복합 UK/상태 CHECK 유지 | 상태/일시 suffix | 보유 도서 조회 영향 |
| `collection` | 11개 컬럼 | `COLLECTION_ID`, `CREATED_BY`, `COLLECTION_NM`, `COLLECTION_TP`, `DISCOUNT_RATE`, `COLLECTION_DESC`, `START_DT`, `END_DT`, `COLLECTION_ST`, `CREATED_DTTM`, `UPDATED_DTTM` | 컬렉션 | PK/FK `CHAR(36)` | 유형/상태 CHECK | 날짜/상태/설명 suffix | 컬렉션 API 영향 |
| `collection_book` | 5개 컬럼 | `COLLECTION_BOOK_ID`, `COLLECTION_ID`, `BOOK_ID`, `DISPLAY_ORD`, `CREATED_DTTM` | 컬렉션 도서 | PK/FK `CHAR(36)` | 복합 UK 유지 | 순서/일시 suffix | 컬렉션 정렬 영향 |
| `notification` | 9개 컬럼 | `NOTIFICATION_ID`, `MEMBER_ID`, `NOTIFICATION_TP`, `NOTIFICATION_TITLE`, `TARGET_TP`, `TARGET_ID`, `READ_YN`, `CREATED_DTTM`, `READ_DTTM` | 알림 | PK/FK `CHAR(36)`, YN `CHAR(1)` | 소문자 enum 대문자화 | 유형/여부/일시 suffix | 알림 API 영향 |
| `notice` | 7개 컬럼 | `NOTICE_ID`, `NOTICE_TITLE`, `NOTICE_CONTENT`, `NOTICE_ST`, `NOTIFY_YN`, `CREATED_DTTM`, `UPDATED_DTTM` | 공지 | PK `CHAR(36)`, YN `CHAR(1)` | 상태 CHECK | 상태/여부/일시 suffix | 공지 API 영향 |
| `user_reading_statistic` | 11개 컬럼 | `USER_STAT_ID`, `MEMBER_ID`, `PERIOD_TP`, `PERIOD_START_DT`, `PERIOD_END_DT`, `RENTAL_CNT`, `READ_BOOK_CNT`, `READ_PAGE_CNT`, `REVIEW_CNT`, `FAVORITE_CNT`, `UPDATED_DTTM` | 사용자 독서 통계 | PK/FK `CHAR(36)` | 복합 UK 유지 | 유형/날짜/건수 suffix | 통계 API 영향 |
| `admin_statistic` | 16개 컬럼 | `ADMIN_STAT_ID`, `PERIOD_TP`, `AGE_BAND`, `PERIOD_START_DT`, `PERIOD_END_DT`, `TOTAL_MEMBER_CNT`, `SANCTIONED_MEMBER_CNT`, `ACTIVE_BOOK_CNT`, `TOTAL_RENTAL_CNT`, `TOTAL_READ_BOOK_CNT`, `FREE_RENTAL_CNT`, `PAID_RENTAL_CNT`, `TOTAL_PAYMENT_AMT`, `REVIEW_CNT`, `REPORT_CNT`, `UPDATED_DTTM` | 관리자 통계 | PK `CHAR(36)`, 누적 금액 `BIGINT` 가능 | 복합 UK/CHECK 유지 | 건수/금액/날짜 suffix | 관리자 통계 영향 |
| `admin_audit_log` | 8개 컬럼 | `AUDIT_LOG_ID`, `ADMIN_MEMBER_ID`, `ACTION_TP`, `TARGET_TP`, `TARGET_ID`, `BEFORE_SUMMARY`, `AFTER_SUMMARY`, `CREATED_DTTM` | 관리자 감사 로그 | PK/FK `CHAR(36)`, `TARGET_ID`는 대상별 UUID 문자열 검토 | INDEX 유지 | 유형/일시 suffix | 감사 로그 API 영향 |

변경하지 않는 주요 판단:

- `ISBN`, `URL`, `EMAIL`처럼 표준 단어 자체가 명확한 항목은 테이블 접두어를 붙이지 않는다.
- 복합 PK를 가진 N:M 테이블은 구조를 유지하고 FK 타입만 `CHAR(36)`으로 바꾼다.
- CHECK 허용값은 근거 없이 새로 만들지 않고 기존 허용값을 대문자/변경 컬럼명 기준으로 유지한다.

## 5.12 DDL/DML/API 영향도

| 항목 | 현재 상태 | 변경 계획 | 실제 적용 시 영향받는 파일 | 이번 요청 수정 여부 |
|---|---|---|---|---|
| `MEMBER_ID`, `LOGIN_ID` 공존 | xlsx/DDL/API/코드에 둘 다 존재 | `LOGIN_ID` 삭제, `MEMBER_ID`만 내부 식별자 | schema.sql, Entity, DTO, Repository, API, 화면, 테스트 | 수정하지 않음 |
| 로그인 API의 `loginId` | `API-AUTH-002`, `LoginRequest`, `AuthService.findByLoginId` 사용 | 이메일 로그인으로 계획 | API 명세, 화면, DTO, AuthService, Repository | 수정하지 않음 |
| 회원가입 아이디 입력 | U-002 화면과 API-AUTH-001에 아이디 입력 | 이메일을 로그인 식별자로 계획 | 화면설계, API, SignupRequest | 수정하지 않음 |
| 아이디 찾기 기능 | API-AUTH-003, U-004 존재 | `LOGIN_ID` 삭제 시 `계정 확인` 기능으로 대체 계획 | API/화면/AuthService/FindIdResponse | 수정하지 않음 |
| `PASSWORD_HASH`/암호화 비밀번호 | xlsx는 `password_hash`, 설명은 암호화 비밀번호 | `MEMBER_PW`, 논리명 회원 비밀번호 | DDL/Entity/AuthService | 수정하지 않음 |
| `MEMBER_ROLE`과 CHECK의 `ROLE` 참조 | xlsx CHECK가 `role`, 실제 DDL은 `role` | `ADMIN_YN` 또는 CHECK 컬럼명 일치 | CHECK/인증/인가 | 수정하지 않음 |
| `USER/ADMIN`과 향후 `ADMIN_YN` | API/화면/Security는 role 기반 | 검토용 xlsx는 `ADMIN_YN` | SecurityConfig, JWT, 권한 체크 | 수정하지 않음 |
| `STATUS`와 향후 `_ST` | xlsx/DDL/API에 status/status 계열 혼재 | 상태 컬럼 `_ST` 통일 | SQL, Entity, Query, DTO, 테스트 | 수정하지 않음 |
| `_AT`과 향후 `_DTTM` | 대부분 `_at`/`createdAt` 사용 | xlsx는 `_DTTM` | SQL, Java field, API response | 수정하지 않음 |
| `BIGINT UNSIGNED`와 향후 `CHAR(36)` | 모든 xlsx PK/FK가 `BIGINT UNSIGNED` | PK/FK UUID `CHAR(36)` | 전체 DB/JPA/API/테스트 | 수정하지 않음 |
| JPA `Long`과 향후 UUID | Entity/DTO/Repository 전반에서 `Long` | 향후 UUID/string 타입 검토 | Java 전체 | 수정하지 않음 |
| `MEMBER_EMAIL`과 향후 `EMAIL` | xlsx는 `member_email`, 실제 DDL/API는 `email` | `EMAIL` 통일 | DB/API/화면/DTO | 수정하지 않음 |
| `VARCHAR(255)`와 향후 이메일 `VARCHAR(254)` | xlsx/DDL은 255 | `VARCHAR(254)` 계획 | DDL/검증 | 수정하지 않음 |
| `GRADE_LEVEL`과 향후 `GRADE_ORD` | xlsx/DDL/DML/Java `grade_level`/`gradeLevel` | `GRADE_ORD` | data.sql, MemberGrade, 등급 로직 | 수정하지 않음 |
| DDL/DML의 소문자 enum 값 | `challenge_reward`, `coupon`, `payment`, `notice`, `book_request` 등 | 대문자 enum | DDL/data/test/API | 수정하지 않음 |
| CHECK 내부의 변경 전 컬럼명 | DDL 인덱스 일부가 `(status)`, `(title)` 등 실제 컬럼과 불일치 | 변경 컬럼명과 일치 | schema.sql | 수정하지 않음 |
| 스프레드시트에 없고 DDL에만 있는 임시 테이블 | `book_isbn_temp` 존재 | 검토용 ERD 포함 여부 별도 결정 | batch/domain/repository/schema | 수정하지 않음 |

## 5.13 문서 간 정책 충돌

| 충돌 항목 | 문서 A | 문서 B | 계획 문서의 적용 방향 | 실제 적용 전 추가 결정 |
|---|---|---|---|---|
| 회원 물리 삭제 여부 | 0704 멘토링: 한 달 이후 물리 삭제 | final API/화면: `WITHDRAWN` 상태, 물리 삭제하지 않음 | 물리 삭제하지 않고 상태 유지와 비식별화 컬럼을 유지 | 실제 DDL에는 미적용 |
| 개인정보 비식별화 | ERD: `anonymized_at` 의미 확인 필요 | final 정책: 탈퇴 상태 유지 | `ANONYMIZED_DTTM` 유지 계획 | 비식별화 시점/대상 결정 |
| 비밀번호 재설정 기능 | xlsx: `password_reset_code` 시트 존재 | final API/화면: 비회원 비밀번호 재설정 제외 | 기존 `password_reset_code` 시트 하나를 유지하고 해당 시트만 수정 | 42개 시트 유지 기준으로 검증 |
| 로그인 아이디 삭제와 이메일 로그인 | ERD 피드백: `LOGIN_ID` 삭제 방향 | API/화면/코드: 아이디 로그인/아이디 찾기 | 검토용 xlsx는 EMAIL 로그인 계획. 기존 아이디 찾기는 `계정 확인`으로 대체 | 화면/API 명칭과 응답 의미 변경 영향 기록 |
| Google 로그인 | ERD 피드백: Google 로그인 고려 | 현재 final 문서: 구체 구조 없음 | 별도 인증 테이블 없이 `MEMBER.GOOGLE_SUB` 컬럼만 추가 계획 | 다중 소셜 제공자 확장 시 별도 인증 구조 필요 |
| `ROLE`과 `ADMIN_YN` | ERD 피드백: 관리자 여부 | API/Security: `USER/ADMIN` role | `ADMIN_YN` 계획, 충돌 기록 | 실제 DDL에는 미적용 |
| 쿠폰 만료 여부 | 0704 메모: 쿠폰 유효기간 없애라 의견 | final API/화면: 쿠폰 만료일 표시 | 쿠폰 만료일/유효기간 컬럼 제거 방향 | 쿠폰 자체 삭제가 아니라 만료 정책 제거로 해석 |
| 대문자 테이블명 | ERD 피드백: 전체 대문자 | 실제 DDL/JPA: 소문자 | 검토용 xlsx는 대문자 | 실제 DB 적용 여부 결정 |
| UUID PK | ERD 피드백: 정수형 PK 제외 | 실제 DDL/JPA: BIGINT/Long | 검토용 xlsx는 UUID `CHAR(36)` | 실제 DDL에는 미적용 |
| `_ST` 상태 suffix | 멘토: 컬럼명 축약 | 실제 DDL/API: status | 검토용 xlsx는 `_ST` | 표준단어 확정 |
| `_AT`과 `_DTTM` | 멘토: `birth_DT` 등 축약 | 실제 Java/API: `createdAt` | xlsx는 `_DTTM`, API는 camelCase 유지 가능 | 물리명/API명 변환 규칙 결정 |
| 실제 DDL과 검토용 스프레드시트 차이 | DDL은 실행 기준 | xlsx는 멘토 검토용 설계 | 차이를 숨기지 않고 영향도 기록 | 실제 적용 단계 별도 계획 |

## 5.14 다음 단계의 xlsx 생성 기준

이번 요청에서는 xlsx를 생성하지 않는다. 다음 단계에서 생성할 경우 예정 파일명은 `ebook-rental-table-layout-feedback-reflected.xlsx`다.

다음 단계에서는 원본을 덮어쓰지 않고 복제본을 생성한다.

보존 대상:

- 기존 42개 시트
- 시트 이름
- 시트 순서
- 병합 셀
- 글꼴
- 색상
- 테두리
- 정렬
- 열 너비
- 행 높이
- 인쇄 설정
- 전체 레이아웃

수정 예정 대상:

- 컬럼 순번
- 테이블명
- 컬럼명
- 논리명
- 설명
- 자료형
- NULL
- PK/FK/UK/CHECK
- 인덱스
- 초기값
- 비고

## 5.15 다음 단계의 자동 검증 기준

실제 xlsx를 생성하는 다음 단계에서만 자동 검증을 수행한다. 이번 요청에서는 실행하지 않는다.

- 시트가 42개인지
- 시트 순서가 원본과 같은지
- 모든 물리명이 대문자 snake_case인지
- PK/FK가 `CHAR(36)`인지
- PK/FK에 INT 또는 BIGINT가 남았는지
- `AUTO_INCREMENT`가 남았는지
- `UNSIGNED`가 남았는지
- `LOGIN_ID`가 남았는지
- 상태 컬럼이 `_ST`인지
- CHECK가 실제 변경 컬럼을 참조하는지
- 소문자 enum이 남았는지
- PK/FK/UK/CHECK가 누락됐는지
- 복합키와 복합 UNIQUE가 유지됐는지
- 스타일과 인쇄 설정이 유지됐는지

## 5.16 아직 확정하지 않은 항목

| 항목 | 현재 문서 상태 | 선택 가능한 대안 | 이번 계획의 임시 적용 방향 | 실제 적용 전 결정할 내용 |
|---|---|---|---|---|
| 성별 코드 허용값 | xlsx는 `gender`, 허용값 없음 | 유지, 삭제, `GENDER_CD` 코드화 | `GENDER_CD CHAR(1) NULL`, 허용값 `M`, `F` | 비필수 입력이며 `M/F` 외 값은 허용하지 않음 |
| UUID 버전 | UUID 생성 방식의 종류를 묻는 항목. v4는 무작위 UUID, v7은 시간 정렬성을 고려한 UUID | v4, v7, JPA 기본 전략 | PK/FK 저장 타입은 `CHAR(36)`으로 확정하고, UUID 버전은 설명만 남김 | 이번 계획 단계에서 버전 확정이 필요한지 확인 |
| 운영 DB PK/FK 저장 타입 | PK와 이를 참조하는 FK의 UUID 저장 형태를 묻는 항목 | `CHAR(36)`, `BINARY(16)` | PK와 FK는 `CHAR(36)`으로 계획 | `BINARY(16)`은 효율 대안으로만 기록하고 미선택 |
| 회원 물리 삭제 여부 | 문서 충돌 | 물리 삭제, 상태 유지, 비식별화 | 물리 삭제하지 않고 `WITHDRAWN` 상태와 비식별화 컬럼 유지 | 실제 DDL에는 미적용 |
| Google 로그인 인증 구조 | 별도 구조 없음 | `GOOGLE_SUB` 단일 컬럼, 추가 컬럼, 별도 auth 테이블 | 별도 테이블 없이 `MEMBER.GOOGLE_SUB` 컬럼만 추가하고 `GOOGLE_LINKED_DTTM`, `LOGIN_PROVIDER_CD`는 추가하지 않음 | 다중 소셜 제공자 확장 여부 |
| 아이디 찾기 기능의 대체 여부 | 현재 화면/API에 아이디 찾기 존재 | 기존 유지, 이메일 로그인 도움말로 변경, 계정 확인으로 변경 | `LOGIN_ID` 삭제 계획을 유지하고 기존 아이디 찾기는 `계정 확인`으로 대체 | 화면/API 명칭과 응답 의미 변경 영향 기록 |
| 이메일 변경 정책 | API는 이메일 수정 가능 | 변경 허용, 인증 후 변경, 변경 금지 | 이메일 변경 불가 | 화면/API와 충돌 기록 |
| 이메일 대소문자 처리 | 미정 | 원문 보존+정규화 비고, 소문자 저장 컬럼, DB collation 의존 | `EMAIL_NORM` 실제 컬럼은 만들지 않고 `EMAIL` 원문 보존과 애플리케이션 검증/비고 정책으로 관리 | 대소문자 중복 방지 근거와 검증 방식 |
| 관리자 여부와 역할 체계 | role 사용 중 | `ADMIN_YN`, `ROLE_CD`, 권한 테이블 | `ADMIN_YN`으로 계획 | 실제 DDL에는 미적용 |
| 비밀번호 재설정 코드 시트 | 기존 시트 존재 | 기존 시트 직접 수정, 새 시트 추가 | 새 시트를 추가하지 않고 기존 `password_reset_code` 시트 하나만 수정 | 기존 시트명과 42개 시트 유지 검증 |
| 쿠폰 만료 정책 | final 문서는 쿠폰 만료일 표시, 0704 메모는 만료 제거 의견 | 만료일 유지, 만료일 제거 | 쿠폰 만료일과 유효기간 컬럼 제거 방향 | 쿠폰 화면/API 영향도 기록 |
| 실제 DDL 적용 시점 | 실제 DDL은 변경하지 않음 | 미적용, 별도 마이그레이션 | 실제 DDL 미적용 | 이 계획 문서만 수정 |
| 도서관정보나루 API 매뉴얼 반영 | 파일 없음 | 반영, 미반영 | 반영하지 않음 | 다음 단계에서도 제외 |

멘토 txt 직접 지적 외 추가로 근거가 필요한 부분:

| 항목 | 이상하거나 근거가 필요한 이유 | 다음 단계에서 확인할 근거 |
|---|---|---|
| 아이디 찾기와 `LOGIN_ID` 삭제 | 사용자가 설명한 아이디 찾기는 이메일 인증 후 기존 아이디를 조회하는 흐름인데, `LOGIN_ID`를 삭제하면 조회할 아이디가 사라진다. | `LOGIN_ID` 삭제를 유지하고 기능명을 `계정 확인`으로 바꾸는 근거 |
| `GOOGLE_SUB`를 회원 테이블에 두는 방식 | Google 단일 연동에는 단순하지만, 여러 소셜 로그인을 확장하면 회원 테이블 컬럼이 늘어난다. | 단일 Google 연동만 고려한다는 범위 근거, 다중 제공자 확장 시 별도 인증 구조 대안 |
| `EMAIL_NORM` 컬럼 미생성 | 실제 컬럼을 만들지 않으면 대소문자 중복 방지를 애플리케이션 검증이나 DB collation에 의존한다. | 이메일 UNIQUE와 대소문자 처리 방식의 근거 |
| 쿠폰 만료 컬럼 제거 | 쿠폰 만료 표시가 있는 화면/API와 충돌한다. | 쿠폰 만료 정책 제거 이유와 영향도 |
| 비밀번호 재설정 코드 기존 시트 수정 | final API 범위에서는 제외됐지만 멘토 피드백 대상이므로 시트를 삭제하지 않는다. | 새 시트를 만들지 않고 기존 `password_reset_code` 시트 하나만 수정하는 근거 |

## 5.17 다음 검토 시 확인할 체크리스트

- UUID 버전은 이번 계획 단계에서 확정할 필요가 있는지 확인
- 기존 아이디 찾기를 `계정 확인`으로 바꾸는 근거와 화면/API 영향도를 정리했는지 확인
- Google 로그인은 `GOOGLE_SUB` 단일 컬럼으로 충분하다는 근거를 정리했는지 확인
- 이메일 정규화는 실제 컬럼 없이 비고/검증 정책으로 관리한다는 근거를 정리했는지 확인
- `ADMIN_YN` 전환과 기존 `USER/ADMIN` 역할 차이를 비고에 충분히 설명했는지 확인
- 실제 DDL 미적용 전제를 영향도 표에 유지했는지 확인
- 비밀번호 재설정 코드는 기존 `password_reset_code` 시트 하나만 수정하고 42개 시트를 유지하는지 확인
- 쿠폰 만료 컬럼 제거가 쿠폰 관련 테이블과 CHECK, API/화면 영향도에 모두 반영됐는지 확인
- xlsx 대문자 물리명과 실제 DDL 소문자 물리명의 차이를 허용할지
- 다음 단계에서 `ebook-rental-table-layout-feedback-reflected.xlsx`를 생성할지
- 모든 테이블과 속성명, 자료형, NULL, PK/FK/UK/CHECK/INDEX, 초기값, enum 허용값마다 변경 근거가 남았는지 확인

## 향후 설계 결정 근거서 작성 계획

- 향후 예상 파일명: `2026-07-10_ebook-rental-table-layout-설계결정근거.md`
- 실제 xlsx 생성 전 또는 생성 후 별도 요청으로 작성한다.
- 예상 질문과 답변을 포함한다.
- 각 결정의 공식 근거를 포함한다.
- 장점뿐 아니라 단점과 대안을 포함한다.
- 피드백 직접 반영과 기술적 권장안을 구분한다.
- 다음 단계에서 스프레드시트를 만들 때 모든 테이블과 속성명, 자료형, NULL 여부, PK/FK/UK/CHECK/INDEX, 초기값, CHECK 허용값, enum 대소문자 변경마다 근거를 같이 수집한다.
- 근거는 멘토 피드백, 현재 xlsx, DDL/DML, API/화면 문서, 공식 문서, DB 표준화 원칙 중 어느 것에 해당하는지 구분한다.

근거 수집 계획:

| 근거 대상 | 확인할 내용 | 문서화할 결정 근거 |
|---|---|---|
| 테이블명 | 대문자 snake_case, 업무 의미, 실제 DDL과 차이 | 멘토 피드백 반영인지, 표준화 결정인지 구분 |
| 속성명 | suffix, 약어 사전, 중복 접두어 여부 | 왜 해당 물리명과 논리명을 선택했는지 기록 |
| 자료형 | 길이, 숫자 범위, 날짜/일시 구분, UUID 저장 방식 | 공식 문서 또는 업무 범위 근거 기록 |
| NULL/기본값 | 필수 입력, 선택 입력, 기본 상태값 | API/화면 입력 정책과 충돌 여부 기록 |
| PK/FK/UK | UUID `CHAR(36)`, 복합키 유지, 참조 타입 일치 | 정수형 PK 제외 피드백과 참조 무결성 근거 기록 |
| CHECK/enum | 허용값, 대문자화, 컬럼명 변경 후 CHECK 참조 | 기존 허용값 유지 여부와 변경 이유 기록 |
| INDEX | 검색 조건, UNIQUE와 중복 여부 | API/화면 조회 조건에 근거한 인덱스 여부 기록 |
| 초기값 | 상태값, Y/N, 포인트 등 기본값 | DML/API 기본 정책과 충돌 여부 기록 |
| 삭제/제외 컬럼 | `LOGIN_ID`, 쿠폰 만료 컬럼, `EMAIL_NORM` 등 | 삭제 이유, 대안, 영향도 기록 |

예상 질문 목록:

1. 왜 BIGINT 대신 UUID인가?
2. 왜 `CHAR(36)`인가?
3. 왜 `BINARY(16)`을 사용하지 않았는가?
4. UUID가 인덱스 성능에 불리하지 않은가?
5. 왜 `LOGIN_ID`를 삭제했는가?
6. 사용자는 무엇으로 로그인하는가?
7. EMAIL 로그인은 적절한가?
8. Google 로그인은 어떻게 처리하는가?
9. 왜 `GOOGLE_SUB`만 두고 `GOOGLE_LINKED_DTTM`, `LOGIN_PROVIDER_CD`는 두지 않는가?
10. 왜 `EMAIL_NORM`을 실제 컬럼으로 만들지 않는가?
11. 아이디 찾기와 `LOGIN_ID` 삭제 계획은 어떻게 양립하는가?
12. 회원 비밀번호라고 쓰면 원문 저장으로 오해되지 않는가?
13. 왜 암호화 비밀번호라고 쓰지 않았는가?
14. 왜 `STATUS`가 아니라 `_ST`인가?
15. 왜 전체 대문자인가?
16. 왜 쿠폰 만료일과 유효기간 컬럼을 제거하는가?
17. 왜 비밀번호 재설정 코드는 기존 `password_reset_code` 시트 하나만 수정하는가?
18. 실제 코드와 설계 문서가 다른 이유는 무엇인가?
19. 이 설계를 실제 DB에 바로 적용할 수 있는가?

이번 요청에서는 질문에 대한 완성된 답변 문서를 만들지 않고, 향후 작성 범위만 기록한다.





