# AWS RDS·EPUB 저장 구조·비용 검토보고

- 작성일: 2026-07-20
- 대상: ABC 팀 프로젝트의 서울 리전 RDS for MySQL 및 예정된 AWS 배포 환경
- 목적: 외부 도서 API 수집과 EPUB 제공 구조를 기준으로 초기 RDS 용량, 생성 설정, 스키마 변경 시점, 비용 발생 지점을 결정한다.
- 범위: 검토와 권고만 한다. RDS 생성, AWS 결제 플랜 전환, 스키마·코드 변경은 이 문서 범위에 포함하지 않는다.

## 1. 결론

1. 전자책 원본은 RDS에 넣지 않고 S3에 저장하며, RDS에는 S3 객체 키 또는 URL만 저장한다.
2. 이 구조와 2026-07-27까지의 팀 개발·데모 목적이라면 RDS MySQL Single-AZ의 **General Purpose SSD(gp3) 20 GiB**로 시작해도 용량 부족 위험은 낮다.
3. 현재 코드에는 `book_page.page_content`에 본문을 저장하는 흐름이 남아 있다. EPUB 방식으로 전환할 때 이 경로를 제거 또는 비활성화하지 않으면 본문이 계속 RDS에 저장되므로, 이 결정이 실제로 반영된 것이 아니다.
4. 컬럼 추가·제약 변경은 **RDS 생성 전**에 `schema.sql`과 JPA 엔티티·DTO·서비스를 함께 확정하는 편이 가장 안전하다. 로컬 파일과 로컬 DB에서의 변경은 AWS 비용을 발생시키지 않는다.
5. RDS 생성 후 DDL 실행에 별도 "컬럼 추가 요금"은 없다. 다만 데이터·인덱스 증가에 따른 **할당 스토리지 비용**, 변경 중의 DB CPU/성능 저하, 대형 변경의 잠재적 잠금·운영 위험은 발생할 수 있다.

## 2. 확인한 현재 상태

| 구분 | 확인 내용 | 판단 |
|---|---|---|
| 외부 도서 API 1차 범위 | ISBN13 단건 조회 → `book_isbn_temp` upsert → 관리자 승인 후 `book` 이관 | 대량 수집이 아니므로 메타데이터 저장량은 작다. |
| 외부 API 저장값 | 제목·저자·출판사·설명·표지 URL·카테고리 참고값 | 이미지 파일을 DB에 넣지 않는다. URL과 텍스트만 저장한다. |
| 현재 본문 테이블 | `book_page.page_content LONGTEXT NOT NULL` | 본문을 넣으면 RDS 용량의 실제 위험 지점이다. |
| 현재 승인 요청 | `pages[]`의 `pageContent`를 받을 수 있고 페이지 수·본문 크기 상한이 없다 | EPUB 전환 전까지는 대용량 본문 유입을 막지 못한다. |
| SQL 초기화 | `spring.sql.init.mode=always`, `schema.sql` 실행, JPA는 `validate` | 생성 후 변경을 스키마 파일에 추가할 때 재실행 안전성(idempotency)을 반드시 고려해야 한다. |

근거:

- `docs/구현계획/외부도서API_통합계획_검증.md` 11~37, 202~215, 340~368행
- `src/main/resources/schema.sql` 12~30, 251~260행
- `src/main/java/com/acorn/abc/admin/book/dto/BookIsbnTempApproveRequest.java` 41~64행
- `src/main/resources/application.properties` 7~23행

## 3. EPUB 저장 최종 권장 구조

```text
관리자 업로드 또는 배포 파이프라인
        │ EPUB 바이너리
        ▼
Amazon S3 버킷 (예: abc-book-content)
        │ 객체 키/URL만 저장
        ▼
RDS book 또는 별도 book_file 테이블
        │
        ▼
백엔드가 권한·대여 상태 검증 후 S3 접근 경로를 제공
```

### 3.1 RDS에 저장할 값

- 권장: `epub_s3_key VARCHAR(500) NOT NULL`
  - 예: `books/{bookId}/original.epub`처럼 버킷을 제외한 S3 객체 키를 저장한다.
  - 버킷명·리전·CDN 도메인을 바꿔도 DB 값을 일괄 변경하지 않아도 된다.
- 선택: `epub_content_type VARCHAR(100) NOT NULL DEFAULT 'application/epub+zip'`
- 선택: `epub_size_bytes BIGINT UNSIGNED NULL`
- 선택: `epub_uploaded_at DATETIME NULL`

`epub_s3_key`는 파일 경로 문자열일 뿐이므로 수백~수천 권 수준에서도 RDS 용량에 의미 있는 부담이 되지 않는다. 표지 이미지도 S3에 직접 저장하는 경우 동일하게 객체 키/URL만 DB에 저장한다.

### 3.2 테이블 선택 원칙

| 방식 | 적합한 경우 | 권장 제약 |
|---|---|---|
| `book`에 EPUB 컬럼 추가 | 책당 EPUB 원본이 정확히 한 개이고 파일 이력·여러 형식이 필요 없을 때 | `epub_s3_key`에 `UNIQUE`, `NOT NULL` 여부는 기존 도서의 파일 유무 정책에 맞춰 결정 |
| 별도 `book_file` 테이블 | EPUB/PDF/샘플 파일·개정 이력·다중 파일을 지원할 가능성이 있을 때 | `book_id` FK, `(book_id, file_type)` UNIQUE, `s3_key` UNIQUE, 파일 상태값 CHECK |

현재 스키마에는 `book_file` 테이블이 없고 `book_page`가 본문을 가진다. EPUB 전환을 구현할 때는 **두 구조를 동시에 장기간 운영하지 않는 것**을 원칙으로 한다. 단, 기존 `book_page` 데이터를 보존해야 한다면 별도 마이그레이션 계획과 읽기 전환 기간이 필요하다.

## 4. RDS 생성 권장 설정

| 생성 화면 항목 | 권장 값 | 이유/비용 영향 |
|---|---|---|
| 리전 | Asia Pacific (Seoul), `ap-northeast-2` | 팀과 서비스 대상이 한국이면 지연 시간과 리전 간 전송을 줄인다. |
| 템플릿 | Free tier 우선 | 자격·크레딧이 남아 있다면 먼저 사용한다. 유료 전환은 필수가 아니다. |
| 엔진 | RDS for MySQL, MySQL 8.4 최신 지원 마이너 버전 | MySQL 8.0은 2026-07-31 표준 지원 종료 예정이므로 신규 생성 대상으로 권장하지 않는다. |
| 가용성/내구성 | Single-AZ DB instance | 요구사항인 인스턴스 1개와 일치한다. Multi-AZ 대기 인스턴스 비용은 피하지만 자동 장애 조치는 없다. |
| 인스턴스 유형 | `db.t4g.micro` | 단기 개발/데모의 저비용 시작점이다. 동시 사용자·통계 작업이 많아 부족할 때만 상향한다. |
| 스토리지 유형 | General Purpose SSD, `gp3` | 새 구성의 일반 용도 선택이다. Provisioned IOPS(io1/io2)는 사용하지 않는다. |
| 할당 스토리지 | **20 GiB** | 메타데이터·S3 경로 저장 구조에는 충분하다. |
| 스토리지 자동 조정 | 기본값은 **끔** | EPUB 원본을 DB에 저장하지 않으므로 예측 가능한 20 GiB가 적합하다. 예상 밖 대량 적재를 허용해야 한다면 최대 30~50 GiB로 켜되, 증가분은 과금되고 다시 줄일 수 없다. |
| 초기 DB 이름 | `ABC` | 현재 기본 JDBC URL의 DB 이름과 일치한다. |
| 백업 보존 기간 | 7일 | 7월 27일까지의 단기 운영 중 복구 지점을 확보한다. |
| 삭제 방지 | 사용 | 기간 중 실수로 DB를 삭제하는 위험을 낮춘다. 종료 시 해제해야 삭제할 수 있다. |
| 자동 마이너 업그레이드 | 사용 | MySQL 8.4 마이너 보안 패치를 유지관리 창에 반영한다. |
| 퍼블릭 액세스 | 팀 로컬 Workbench 연결 기간에만 예 | 보안 그룹 인바운드는 각 팀원의 `My IP`만 TCP 3306으로 허용하고 `0.0.0.0/0`은 금지한다. 배포 후에는 비공개 RDS와 애플리케이션 보안 그룹 허용으로 전환한다. |
| RDS Proxy | 생성 안 함 | 현재 Spring Boot의 커넥션 풀과 단일 소형 개발 DB에 추가 비용·Secrets Manager 관리 복잡도만 늘린다. |
| 모니터링 | 기본 CloudWatch 지표 사용 | `FreeStorageSpace`, CPU, DB connections 알람을 설정한다. |

## 5. 비용은 어디에서 발생하는가

### 5.1 RDS와 스키마 변경

| 행위 | 별도 비용 발생 여부 | 실제 비용/위험 |
|---|---|---|
| 로컬에서 컬럼·제약·엔티티·DDL 수정 | 발생하지 않음 | AWS 리소스를 만들지 않으므로 AWS 과금이 없다. |
| RDS 생성 전 최종 `schema.sql` 반영 | 발생하지 않음 | 생성 후 첫 초기화에 올바른 스키마가 만들어져 운영 DDL을 줄인다. |
| RDS에서 `ALTER TABLE ADD COLUMN` | DDL 명령 자체의 별도 요금 없음 | 인스턴스가 실행 중인 시간의 비용은 계속 발생한다. 컬럼/인덱스가 늘면 데이터 볼륨이 커져 할당 스토리지·백업 사용량이 늘 수 있다. 대형 테이블은 성능 저하·잠금 가능성이 있다. |
| 인덱스·UNIQUE·FK 추가 | 별도 "제약 요금" 없음 | 인덱스 데이터와 쓰기 작업이 증가한다. 대형 테이블 변경은 임시 공간을 요구할 수 있다. |
| CHECK 변경 또는 컬럼 타입 축소 | 별도 "제약 요금" 없음 | 기존 데이터 정합성 실패, 테이블 재구성, 배포 실패 위험이 있으므로 사전 검증이 필요하다. |
| RDS 할당 스토리지 증가 | 발생함 | 프로비저닝한 GiB가 증가한 시점부터 비례 과금된다. RDS 스토리지는 증가만 가능하고 감소할 수 없다. |
| 자동 백업 보존 기간·수동 스냅샷 증가 | 발생할 수 있음 | 보관 데이터가 늘면 백업 스토리지 사용량이 늘어난다. |

RDS MySQL의 가격 요소는 DB 인스턴스 실행 시간, 프로비저닝 스토리지, 백업/스냅샷, 데이터 전송 등이다. 가격 체계에 컬럼 추가나 제약 추가를 위한 별도 항목은 없다. 단, DDL이 인덱스·데이터 복사·로그를 늘리는 간접 효과는 위 표대로 고려해야 한다.

### 5.2 EPUB/S3

| 항목 | 비용 발생 여부 | 관리 원칙 |
|---|---|---|
| S3에 EPUB 저장 | 발생함 | 저장한 객체의 GB·월 사용량에 따라 과금된다. |
| EPUB 업로드/조회 | 발생함 | PUT, GET, LIST 등의 요청 수에 따라 소액의 요청 비용이 발생한다. |
| 사용자에게 EPUB 전송 | 발생할 수 있음 | 인터넷으로 나가는 데이터 전송량이 핵심이다. 파일을 반복 다운로드/스트리밍하면 비용이 증가한다. |
| S3와 같은 리전의 AWS 서비스 간 전송 | 일반적으로 S3 데이터 전송 비용이 없음 | S3 요금 페이지의 같은 리전 AWS 서비스 전송 조건을 확인하되, 사용하는 서비스의 별도 네트워크 비용은 함께 확인한다. |
| DB의 S3 키/URL 문자열 저장 | 사실상 무시 가능한 수준 | 문자열 자체는 매우 작고 RDS 데이터 용량을 유의미하게 늘리지 않는다. |

S3는 RDS 본문 저장보다 파일 크기에 맞게 확장되고, 파일·요청·전송량이 비용 요소로 분리되어 있어 EPUB 원본 저장 위치로 적합하다. 실제 예상 책 수, 평균 EPUB 용량, 월 다운로드 횟수를 넣어 AWS Pricing Calculator에서 서울 리전 기준으로 산정한다.

### 5.3 배포 시 특히 먼저 확인할 항목

| 서비스/구성 | 비용 발생 여부 | 주의점 |
|---|---|---|
| EC2 또는 ECS Fargate 애플리케이션 | 발생함 | EC2는 인스턴스·EBS·공인 IPv4 등을, Fargate는 요청한 vCPU·메모리·추가 임시 스토리지를 실행 시간 기준으로 과금한다. |
| Application Load Balancer | 발생함 | 실행 시간과 LCU 사용량이 과금된다. 소규모 팀 프로젝트에서 항상 켜 둔 ALB도 비용 항목이 된다. |
| NAT Gateway | 발생함 | 실행 시간과 처리한 GB 모두 과금된다. 비용 급증 가능성이 있어, 단기·저비용 환경에서 필요성을 먼저 검토한다. |
| RDS Proxy | 발생함 | 현재 단일 개발 DB에는 만들지 않는다. |
| CloudWatch Logs | 발생할 수 있음 | 로그 수집량·보존 기간을 제한한다. 디버그 SQL 로그를 장기간 보존하지 않는다. |
| S3 버전 관리·Lifecycle 미설정 | 발생할 수 있음 | EPUB 교체본과 삭제 마커가 누적될 수 있다. 필요 시 이전 버전 만료 규칙을 설계한다. |

## 6. 스키마 변경 시점 권고

### 6.1 RDS 생성 전에 할 작업 — 권장

다음은 RDS를 만들기 전에 확정한다.

1. EPUB 저장 모델: `book` 컬럼 추가인지 `book_file` 테이블 신설인지 결정한다.
2. 컬럼명·NULL 정책·UNIQUE/FK/CHECK·인덱스·삭제 정책을 확정한다.
3. `book_page` 본문 저장을 중단할 경우, 승인 DTO·서비스·뷰어 조회 API·엔티티·스키마를 같은 변경 단위로 정리한다.
4. 실제 MySQL 8.4 호환 DB에서 `schema.sql`을 초기화하고, EPUB 경로가 있는 책/없는 책/중복 경로/권한 없는 뷰어를 검증한다.
5. `schema.sql`과 JPA 엔티티의 컬럼 정의를 맞춘다. 현재 `ddl-auto=validate`이므로 불일치는 애플리케이션 기동 실패로 드러날 수 있다.

이 시점의 장점은 AWS 과금 없이 모델을 여러 번 검토할 수 있고, 생성된 RDS에 운영 중 `ALTER TABLE`을 수행할 필요가 줄어든다는 점이다.

### 6.2 RDS 생성 후 변경해야 할 때

작은 변경도 다음 순서를 따른다.

1. 운영 데이터 백업과 적용 대상 행 수를 확인한다.
2. MySQL 8.4과 동일한 테스트 DB에서 DDL과 애플리케이션을 검증한다.
3. 변경이 테이블 재구성/대량 인덱스 생성인지 확인하고 저사용 시간에 적용한다.
4. 현재 프로젝트는 `spring.sql.init.mode=always`이므로, 재기동마다 실패하거나 중복 실행되지 않는 방식으로 스키마 초기화/마이그레이션을 설계한다.
5. 적용 후 `FreeStorageSpace`, 오류 로그, API 정상·실패 경로를 확인한다.

`ALTER TABLE` 자체가 비싼 기능이어서 미리 해야 하는 것은 아니다. **운영 중단·데이터 정합성·일시적 저장 공간·되돌리기 난이도** 때문에 생성 전에 확정하는 것이 더 좋다.

## 7. 비용 보호 장치

1. AWS Budgets에서 월 예산과 50%, 80%, 100% 알림을 팀 이메일로 설정한다.
2. RDS에는 `FreeStorageSpace` 알람을, S3에는 월 비용/저장량 모니터링을 둔다.
3. `Environment=dev`, `Project=ABC`, `Owner=<team>` 태그를 RDS, S3, 컴퓨팅, 로드밸런서에 공통으로 설정한다.
4. 테스트 종료 후에도 RDS 인스턴스, 수동 스냅샷, S3의 불필요한 EPUB 원본, NAT Gateway, ALB가 남아 있지 않은지 확인한다. RDS를 중지해도 스토리지와 백업은 계속 과금될 수 있다.
5. 실제 배포 전 AWS Pricing Calculator에 서울 리전 기준으로 RDS + S3 + 선택한 컴퓨팅 + ALB/NAT 여부를 함께 넣는다. RDS만 계산하면 전체 비용을 과소평가할 수 있다.

## 8. 확정 전 체크리스트

- [ ] EPUB 바이너리는 S3, DB에는 `epub_s3_key`만 저장한다.
- [ ] `book_page` 본문 저장 경로의 제거/비활성화 범위를 확정한다.
- [ ] `book` 컬럼 추가와 `book_file` 테이블 중 하나를 선택한다.
- [ ] RDS 생성 전 스키마·엔티티·DTO·서비스·API 응답을 함께 맞춘다.
- [ ] RDS는 MySQL 8.4, Single-AZ, `db.t4g.micro`, gp3 20 GiB로 생성한다.
- [ ] `ABC` 초기 DB 이름, 7일 자동 백업, 삭제 방지, 보안 그룹의 팀원 IP 제한을 적용한다.
- [ ] 배포 아키텍처에서 ALB와 NAT Gateway가 정말 필요한지 비용 기준으로 결정한다.
- [ ] AWS Budgets 알림과 리소스 태그를 설정한다.

## 9. AWS 공식 참고 자료

- [RDS for MySQL 가격](https://aws.amazon.com/rds/mysql/pricing/)
- [RDS 스토리지와 Autoscaling](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_PIOPS.Autoscaling.html)
- [RDS 스토리지 변경 제약](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ModifyInstance.Settings.html)
- [RDS for MySQL storage-full 동작](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/MySQL.KnownIssuesAndLimitations.html)
- [Amazon S3 가격](https://aws.amazon.com/s3/pricing/)
- [Amazon ECS/Fargate 가격](https://aws.amazon.com/ecs/pricing/)
- [Application Load Balancer 가격](https://aws.amazon.com/elasticloadbalancing/pricing/)
- [NAT Gateway 가격](https://docs.aws.amazon.com/vpc/latest/userguide/nat-gateway-pricing.html)
- [AWS Budgets 개요](https://docs.aws.amazon.com/whitepapers/latest/cost-optimization-laying-the-foundation/cost-optimization-laying-the-foundation.pdf)
