# ABC Spring Boot AWS 배포 최종 계획

작성 기준일: 2026-07-22  
도메인: `portfoliodev.click`  
주 리전: 서울 `ap-northeast-2`  
CloudFront 인증서 리전: 미국 동부 `us-east-1`

> 이 문서가 최종 실행 기준이다. 기존 Codex/Claude 계획은 검토 이력으로만 보존한다.  
> 현재 단계에서는 AWS 리소스와 애플리케이션 코드를 변경하지 않는다.

## 1. 범위와 확정 결정

### 이번 배포에서 한다

- Spring Boot 운영 설정과 secret 환경변수 분리
- Testcontainers MySQL로 통합테스트를 격리한 뒤 Flyway 기반 빈 RDS 초기화
- EPUB 신규 업로드를 서울 private S3로 전환
- 구매·대여 권한 확인 후 짧은 Presigned URL 발급
- 썸네일을 서울 private S3에 저장하고 CloudFront URL로 공개
- Docker image 생성과 ECR push
- ECS Fargate + ALB + private RDS 배포
- `api.portfoliodev.click`, `assets.portfoliodev.click` HTTPS 적용
- 배포 후 health, log, 권한, 30~50MB 파일 검증

### 이번 배포에서 하지 않는다

- 기존 로컬 DB 데이터 이전
- 기존 DB의 `epub_data` 삭제
- 별도 저장소의 React 배포
- apex/`www` DNS 연결
- 기존 AWS 리소스 변경·삭제
- 승인 전 유료 리소스 생성

### 최종 기술 결정

| 항목 | 결정 |
|---|---|
| 실행 환경 | Docker + ECR + ECS Fargate |
| DB | private RDS MySQL, 최초에는 빈 DB |
| EPUB | 서울 private S3, Presigned GET 300초 이하 |
| 썸네일 | 서울 private S3 + CloudFront OAC 공개 URL |
| schema | Flyway만 사용, 운영 `schema.sql/data.sql` 자동 실행 금지 |
| 네트워크 | 초기에는 NAT 없이 public app subnet의 Fargate + public IP, inbound는 ALB만 |
| HTTPS | API 인증서는 서울, CloudFront 인증서는 `us-east-1`에서 apex + wildcard로 준비 |
| 인프라 관리 | CloudFormation Change Set |
| 배포 image | `latest` 금지, Git SHA tag와 image digest 사용 |

## 2. 확인된 현재 상태

- Spring Boot `4.0.7`, Java `17`, Gradle, MySQL을 사용한다.
- 현재 공통 설정은 `spring.sql.init.mode=always`, `continue-on-error=true`, SQL log 활성 상태다.
- Flyway, Actuator, AWS SDK가 아직 없다.
- `schema.sql`, `data.sql`, `db/manual/*.sql`과 별도 수동 SQL이 공존한다.
- `db/manual`에는 `V001`, `V002`, `U001`, `U002` 파일이 있다.
- DB context를 띄우는 test는 32개다. 그중 31개가 `@AutoConfigureTestDatabase(replace = NONE)`이며, Testcontainers/H2/test profile이 없어 현재는 `localhost:3306/ABC`에 의존한다.
- 따라서 격리 전 전체 test 실행은 개발 DB를 변경할 수 있다. 첫 코드 작업은 Testcontainers MySQL 격리여야 한다.
- `schema.sql`은 현재 additive 변경과 제약을 포함한 최신 통합 schema임을 확인했다.
- `data.sql`의 `member_grade` 4개 행은 운영 필수 참조 데이터이며, 특히 `grade_id=1`은 회원가입 코드가 직접 참조한다.
- EPUB은 최대 50MB이며 `book_epub.epub_data LONGBLOB NOT NULL`에 저장된다.
- 기존 EPUB validator와 구매·대여 권한 검사는 유지할 가치가 있다.
- 썸네일은 현재 `coverImageUrl` 문자열만 저장한다.
- `gradlew`가 Git mode `100644`라 Linux build에서 실행 권한 처리가 필요하다.
- `.gitattributes`에는 이미 `/gradlew text eol=lf`가 있으므로 Dockerfile에서 `chmod +x`만 보장하면 된다.
- `.env.txt`는 출력하거나 문서에 복사하지 않는다.
- `abc-front/` 삭제는 사용자 변경이므로 건드리지 않는다.
- AWS에는 도메인 외 리소스가 없는 것으로 알려졌지만 CLI로 다시 확인해야 한다.

## 3. 배포 구조

```text
Client
  -> https://api.portfoliodev.click
  -> Route 53 Alias
  -> public ALB
  -> ECS Fargate :8080
       -> private RDS MySQL
       -> S3 Gateway Endpoint
            -> private EPUB bucket (Seoul)
            -> private thumbnail bucket (Seoul)

Client
  -> https://assets.portfoliodev.click/covers/...
  -> CloudFront + OAC
  -> private thumbnail bucket (Seoul)
```

| 도메인 | 연결 | 상태 |
|---|---|---|
| `api.portfoliodev.click` | ALB | 이번 배포 |
| `assets.portfoliodev.click` | CloudFront | 이번 배포 |
| `portfoliodev.click` | 향후 React | 이번에는 미연결 |
| `www.portfoliodev.click` | 향후 React | 이번에는 미연결 |

네트워크:

- ALB는 2개 public subnet을 사용한다.
- ECS는 초기 비용 절감을 위해 public application subnet과 public IP를 사용한다.
- ECS Security Group은 ALB Security Group의 8080만 허용한다.
- RDS는 private DB subnet에 두고 ECS Security Group의 MySQL 연결만 허용한다.
- NAT Gateway는 만들지 않는다.
- 서울 S3 Gateway Endpoint를 application route table에 연결한다.
- application public subnet route table의 `0.0.0.0/0 -> Internet Gateway`와 S3 prefix route의 Gateway Endpoint 연결을 Change Set에서 명시적으로 확인한다.
- ECS를 private subnet으로 옮기는 작업은 비용과 Interface Endpoint/NAT 구성을 비교한 후 진행한다.

## 4. 실행 Gate

```text
GATE 0  Git/secret/JDK/Docker 확인, test 실행 없이 testClasses + bootJar
GATE 1  Testcontainers MySQL 격리 + 기존 schema.sql/data.sql 기준 32개 test
GATE 2  Flyway 전환 + Spring Boot/S3 전체 test + bootJar
GATE 3  Docker 로컬 검증
GATE 4  AWS SSO + 기존 리소스 읽기 전용 조사
COST GATE  비용/DNS/IAM/Change Set 사용자 승인
GATE 5  RDS TLS/Flyway one-off 사전검증
GATE 6  서비스 배포 후 health/log/HTTPS/S3 권한 검증
```

앞 Gate가 실패하면 다음 단계로 넘어가지 않는다.

## 5. Spring Boot 작업

### 5.1 작업 전 기준선

1. 현재 `git status --short`를 기록한다.
2. 배포용 별도 브랜치를 사용한다.
3. Java 17과 Docker Desktop을 확인한다.
4. 격리 전에는 전체 test를 실행하지 않는다. compile 기준선만 기록한다.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
docker version
.\gradlew.bat clean testClasses bootJar -x test
```

현재 test는 로컬 MySQL을 건드릴 수 있으므로 `clean test`를 기존 기준선 명령으로 사용하지 않는다. compile 실패 원인이 확정되지 않으면 구현을 시작하지 않는다.

### 5.2 첫 코드 작업: test DB 격리와 dependency/profile

추가:

- Spring Boot Actuator
- Flyway core와 MySQL module
- AWS SDK v2 BOM과 S3
- test scope의 `spring-boot-testcontainers`, Testcontainers JUnit Jupiter/MySQL module

규칙:

- AWS SDK BOM은 구현 시 검증한 정확한 버전으로 고정한다.
- Testcontainers도 Spring Boot `4.0.7`과 호환되는 검증된 정확한 버전으로 고정하고, RDS 후보와 같은 MySQL major/minor image tag를 사용한다.
- `+`, `latest` 같은 동적 dependency 버전을 사용하지 않는다.
- access key를 코드, property, Docker image에 넣지 않는다.
- AWS SDK는 ECS Task Role credential provider를 사용한다.

설정 파일:

```text
application.properties
application-local.properties
application-prod.properties
src/test/resources/application-test.properties
```

test 격리 구조:

1. 공용 `@TestConfiguration` 또는 test용 meta-annotation에 `MySQLContainer`와 `@ServiceConnection`을 선언한다.
2. 32개 DB context test가 모두 이 설정과 `test` profile을 사용하게 한다. 기존 `replace = NONE`은 유지해도 되지만 datasource는 반드시 container connection details가 덮어써야 한다.
3. container는 매 Gradle test JVM에서 하나를 공유해 속도를 확보하되, run 간 재사용은 끄고 매번 새 DB로 시작한다.
4. Docker를 사용할 수 없으면 test를 skip하거나 localhost로 fallback하지 말고 즉시 실패시킨다.
5. guard test에서 JDBC URL이 Testcontainers가 발급한 주소인지, `localhost:3306/ABC`가 아닌지 확인한다.
6. transaction rollback/cleanup을 점검해 test 간 데이터 누출이 없는지 확인한다.

안전한 2단계 전환:

- Pass A: 격리된 MySQL에서 Flyway를 잠시 끄고 현재 `schema.sql + data.sql`로 32개 test의 안전한 기준 결과를 만든다.
- Pass B: `V1/V2/V3` 작성 후 test profile을 `sql.init=never + Flyway`로 바꾸고 같은 32개 test를 다시 실행해 결과를 비교한다.
- Pass A/B 모두 실제 로컬 개발 DB와 RDS에는 접속하지 않는다.

prod 핵심 설정:

```properties
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
server.forward-headers-strategy=framework

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=51MB
spring.servlet.multipart.file-size-threshold=0

management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
management.endpoint.health.probes.enabled=true
management.endpoint.health.group.readiness.include=readinessState,db

springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

최종 test profile은 prod와 동일하게 `spring.sql.init.mode=never`, `spring.flyway.enabled=true`, `ddl-auto=validate`를 사용한다. 차이는 datasource가 Testcontainers MySQL이라는 점뿐이다.

local은 localhost DB와 DB 기반 EPUB fallback을 허용한다. 기존 로컬 DB에는 Flyway baseline을 자동 적용하지 않는다.

### 5.3 Flyway

먼저 다음 파일을 대조해 하나의 canonical schema를 만든다.

```text
schema.sql
data.sql
db/manual/*.sql
src/main/resources의 별도 제약/인덱스 SQL
docs/데이터베이스의 DDL/migration
```

신규 위치:

```text
src/main/resources/db/migration/
  V1__baseline.sql
  V2__reference_data.sql       # member_grade 운영 필수 참조 데이터
  V3__epub_object_storage.sql
```

규칙:

- Flyway 위치를 `classpath:db/migration`으로 고정한다.
- `db/manual`은 과거 이력으로 보존하고 스캔하지 않는다.
- 운영에서는 Flyway와 `schema.sql/data.sql` 자동 실행을 혼용하지 않는다.
- 새 RDS에 `baseline-on-migrate=true`를 사용하지 않는다.
- 최초 migration은 additive만 허용한다.
- 실제 회원, 비밀번호, API key, 개발용 test data를 migration에 넣지 않는다.
- `schema.sql`을 손으로 복사하지 않는다. disposable MySQL에 현재 `schema.sql`을 적용한 뒤 schema-only `mysqldump`로 추출하고, 환경 의존 옵션·자동 증가 현재값 등을 정규화해 `V1`을 만든다.
- `V1`을 현재 `schema.sql`과 diff하고 32개 test 및 Flyway validate로 동등성을 확인한다.
- `V2`에는 `data.sql`을 감사해 확인한 `member_grade` 4개 행(`씨앗`, `새싹`, `나무`, `도토리`)을 반드시 넣는다. `grade_id=1`은 반드시 `씨앗`이어야 한다.
- 각 행의 `grade_level`, 최소 결제액, 기간, 유지율, 설명까지 현재 `data.sql`과 동일하게 보존한다: `씨앗(1/0원/0개월)`, `새싹(2/20,000원/1개월)`, `나무(3/100,000원/3개월)`, `도토리(4/700,000원/6개월)`, 유지율은 모두 `10.00`이다.
- `프론트API연결_확인용_테스트데이터.sql`과 회원/비밀번호 등 확인용 데이터는 `V2`에서 제외한다.

빈 MySQL에서 다음을 확인한다.

1. 최초 migrate
2. JPA validate
3. 재기동 시 migration 재적용 없음
4. Flyway validate
5. repository integration test
6. `member_grade` 4개 행과 회원가입 기본 등급 `grade_id=1` 검증

### 5.4 EPUB schema

`V3__epub_object_storage.sql` 방향:

```sql
ALTER TABLE book_epub
  MODIFY COLUMN epub_data LONGBLOB NULL,
  ADD COLUMN storage_type VARCHAR(16) NOT NULL DEFAULT 'DB',
  ADD COLUMN storage_key VARCHAR(512) NULL,
  ADD COLUMN s3_version_id VARCHAR(128) NULL,
  ADD COLUMN storage_status VARCHAR(16) NOT NULL DEFAULT 'READY',
  ADD CONSTRAINT uk_book_epub_storage_key UNIQUE (storage_key);
```

- 기존 행은 `DB + READY`이며 blob을 유지한다.
- 신규 S3 행은 `S3`이고 `epub_data=NULL`이다.
- 상태는 `UPLOADING`, `READY`, `FAILED`를 사용한다.
- viewer는 최신 `READY` EPUB만 선택한다.
- DB/S3 필수 컬럼 조합은 application validation과 integration test로 보장하고, 선택한 MySQL이 CHECK를 강제하면 DB 제약도 추가한다.
- `epub_data` 컬럼 삭제는 기존 데이터 이전 후 별도 승인 사항이다.

### 5.5 EPUB 저장 구조

현재 concrete `BookEpubBinaryStore` 의존을 다음 계약으로 분리한다.

```text
EpubObjectStorage
  DatabaseEpubObjectStorage   # 기존 행/local 호환
  S3EpubObjectStorage         # 운영 신규 업로드

EpubAccessUrlService
  S3PresignedEpubAccessUrlService
```

업로드 API는 기존 경로를 유지한다.

```text
PUT /api/v1/admin/books/{bookId}/epub
```

업로드 순서:

1. 관리자 권한, book 존재, 크기 확인
2. 기존 EPUB validator와 SHA-256 수행
3. 짧은 DB transaction에서 book lock, version 예약, `UPLOADING` 행 저장
4. transaction 밖에서 S3 upload
5. S3 version ID를 기록하고 `READY`로 전환
6. 성공 후 진행도/북마크 정책 적용
7. 실패 시 해당 S3 object version 삭제 후 행을 `FAILED` 처리하거나 제거

S3 key:

```text
epubs/{bookId}/{epubVersion}/{sha256}.epub
```

이 순서로 S3 upload 중 DB lock 장기 점유와 실패한 새 버전의 노출을 막는다.

### 5.6 EPUB 읽기

신규 API:

```text
GET /api/v1/rentals/{rentalId}/epub-access
```

발급 조건:

- 로그인 사용자와 rental 소유자 일치
- 상태 `READY`, `READING`, `OWNED`
- `OWNED`가 아니면 만료 전
- 최신 `READY` EPUB 존재

응답에는 URL, 만료 시각, 파일명, 크기, checksum, EPUB version을 포함한다.

Presigned URL:

- 기본 300초
- 대여 잔여 시간이 더 짧으면 그 이하로 제한
- 만료 직전/만료 후 발급 금지
- `OWNED`도 짧은 URL 사용
- HTTPS와 필요 시 특정 S3 version ID 사용
- 전체 URL과 signature를 log에 남기지 않음
- API 응답에 `Cache-Control: no-store` 적용 검토

기존 streaming API는 React 사용처를 확인하기 전 삭제하지 않는다.

### 5.7 S3 정책

두 bucket 공통:

- 서울 리전
- Block Public Access
- `BucketOwnerEnforced`
- Versioning, SSE-S3, HTTPS-only
- S3 website hosting 사용 금지
- CloudFormation `Retain`

EPUB Task Role:

```text
s3:PutObject
s3:GetObject
s3:GetObjectVersion
s3:DeleteObject
s3:DeleteObjectVersion
```

권한 resource는 `epubs/*`로 제한한다. 실패 보상은 upload 결과의 정확한 version ID를 삭제한다.

EPUB CORS는 실제 React HTTPS origin의 `GET/HEAD`만 허용한다. CORS는 공개 권한이 아니며 bucket은 계속 private이다.

### 5.8 썸네일

- 관리자 업로드 API 추가
- JPEG/PNG/WebP allowlist
- 확장자, Content-Type, magic byte, 크기, pixel dimension 검사
- key: `covers/{bookId}/{sha256}.{ext}`
- DB에는 `https://assets.portfoliodev.click/covers/...` 저장
- hash key에는 장기 immutable cache header 사용
- S3는 private, CloudFront OAC에만 `GetObject` 허용
- Task Role upload/delete 권한은 `covers/*`로 제한
- 기존 외부 `coverImageUrl`은 유지

외부 URL을 직접 가져와 S3로 옮기는 기능은 SSRF 방어가 필요하므로 이번 작업에서 제외한다.

### 5.9 health와 보안

외부 허용:

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

- ALB는 `/actuator/health/readiness` 사용
- readiness에는 DB 포함, liveness에는 DB/S3 제외
- health 상세 정보 숨김
- 운영 Swagger 비활성
- CORS는 실제 배포된 frontend HTTPS origin만 허용
- JWT, DB secret, API key, Presigned URL log 금지

### 5.10 필수 test

- 32개 DB context test가 매 run의 disposable Testcontainers MySQL만 사용하는지 확인
- Docker 미기동/connection 설정 누락 시 localhost 개발 DB로 fallback하지 않는지 확인
- Pass A의 기존 schema 기준 결과와 Pass B의 Flyway 기준 결과 비교
- 빈 MySQL migrate/validate/재기동
- `member_grade` 4개 seed와 신규 가입자의 기본 `씨앗` 등급
- `db/manual` 미스캔
- 기존 DB EPUB fallback
- S3 upload 성공과 보상 삭제
- 동시 upload version 충돌
- `UPLOADING/FAILED` viewer 제외
- 구매자/유효 대여 성공
- 비로그인/타인/만료 대여 거부
- URL TTL이 대여 만료를 넘지 않음
- Presigned URL log 미노출
- S3 직접 anonymous GET 거부
- 30~50MB 정상 EPUB과 50MB 초과 거부
- 기존 ZIP/XML 보안 validator 회귀
- 썸네일 위장 파일 거부
- Actuator 이외 관리 endpoint 비노출

GATE 2는 전체 `test`와 `bootJar` 성공, secret scan 통과 후 완료한다.

## 6. Docker 작업

파일:

```text
Dockerfile
.dockerignore
compose.yaml
docker/rds/global-bundle.pem
docker/rds/global-bundle.sha256
docker/rds/create-truststore.sh
```

Dockerfile 기준:

- Java 17 multi-stage build
- Gradle Wrapper 사용
- `.gitattributes`가 보장한 `gradlew` LF를 유지하고 Dockerfile에서 `chmod +x` 실행
- runtime은 JRE 17, non-root, port 8080
- `.env*`, `.git`, IDE, test output 제외
- secret을 `ARG/ENV`로 image에 저장하지 않음
- 검증한 고정 base image tag 또는 digest 사용
- AWS 공식 endpoint에서 받은 `global-bundle.pem`과 검토한 SHA-256을 함께 고정하고, builder stage에서 hash 검증 후 모든 CA를 PKCS12 truststore로 가져온다.
- runtime image의 `/opt/app/certs/rds-truststore.p12`를 `-Djavax.net.ssl.trustStore`, `trustStoreType=PKCS12`, `trustStorePassword`로 JVM에 연결한다. truststore에는 공개 CA만 있으므로 store password는 애플리케이션 credential이 아니다.
- `JAVA_TOOL_OPTIONS`의 초기 후보는 `-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError`와 위 truststore 옵션이다. Java 17 기본값에 의존하지 않는다.

로컬 검증:

```powershell
docker build -t abc-api:local .
docker compose up -d
curl.exe http://localhost:8080/actuator/health/readiness
docker compose logs api
docker compose down
```

- compose MySQL은 선택할 RDS MySQL과 맞춘다.
- 기존 개발 DB port/volume을 사용하지 않는다.
- 1GB memory 제한 후보에서 50MB upload를 시험한다.
- JVM 시작 log/`-XshowSettings:vm`으로 1GB 제한 안에서 실제 최대 heap을 확인하고, heap 외 metaspace/direct buffer/thread/OS 여유가 남는지 본 뒤 65~70% 범위에서 확정한다.
- `file-size-threshold=0`으로 multipart를 임시 디스크에 즉시 spool하고, ECS ephemeral storage 사용량도 확인한다.
- `keytool -list`로 truststore의 인증서 수·issuer·만료일을 확인하고 pinned PEM의 hash 불일치를 build 실패로 처리한다.
- image history와 log에 secret이 없어야 한다.

## 7. AWS 준비와 인프라

### 7.1 CLI와 기존 리소스 조사

```powershell
aws configure sso --profile portfolio-admin
aws sso login --profile portfolio-admin
aws sts get-caller-identity --profile portfolio-admin
```

- Identity Center 역할과 대상 계정을 확인한다.
- 기본 리전은 서울로 설정한다.
- 장기 access key와 root 사용자는 사용하지 않는다.
- Route 53, ACM 양 리전, VPC, ECS, ECR, RDS, S3, CloudFront, Secrets, Logs, CloudFormation을 읽기 전용 조사한다.
- secret value는 조회하지 않는다.
- 도메인 외 기존 리소스나 이름 충돌이 발견되면 멈추고 보고한다.

### 7.2 CloudFormation 파일

```text
infra/foundation.yml
infra/edge.yml
infra/regional.yml
infra/parameters.example.json
```

#### Foundation — 서울

- ECR repository, immutable tag, lifecycle
- private EPUB/thumbnail S3 bucket
- 암호화, Versioning, public block, HTTPS-only, Retain

#### Edge — 미국 동부

- `us-east-1` ACM 인증서는 `portfoliodev.click`과 `*.portfoliodev.click` 두 이름으로 DNS 검증한다. 현재는 `assets`만 사용하고 향후 React apex/`www` 배포에서 재사용한다.
- CloudFront OAC와 Distribution의 Alternate Domain Name은 이번에는 `assets.portfoliodev.click`만 지정한다.
- 서울 thumbnail bucket regional domain origin
- HTTPS redirect, Route 53 Alias, cache policy

#### Regional — 서울

- VPC, application/DB subnet, route, S3 Gateway Endpoint
- ALB/ECS/RDS Security Group
- private RDS, managed master secret, JWT secret
- ECS Cluster/Task/Service와 IAM role
- ALB 80 redirect/443 listener와 Target Group
- `api.portfoliodev.click` 서울 ACM 인증서와 Alias
- CloudWatch Logs, deployment circuit breaker

서울 S3와 미국 동부 CloudFront 연결 순서:

1. Foundation에서 bucket 생성
2. Edge에서 bucket regional domain을 parameter로 받아 Distribution 생성
3. Distribution ARN으로 Foundation Change Set을 갱신해 OAC bucket policy 추가
4. Regional stack 배포

3단계의 thumbnail bucket policy는 다음 범위로 고정한다.

```text
Principal  = Service: cloudfront.amazonaws.com
Action     = s3:GetObject
Resource   = arn:aws:s3:::<THUMBNAIL_BUCKET>/covers/*
Condition  = StringEquals:
               AWS:SourceArn = arn:aws:cloudfront::<ACCOUNT_ID>:distribution/<EXACT_DISTRIBUTION_ID>
```

- Distribution ARN에 wildcard를 사용하지 않는다.
- 이 policy는 thumbnail bucket의 `covers/*`에만 적용하며 EPUB bucket에는 적용하지 않는다.
- S3 Block Public Access는 계속 활성화하고 CloudFront OAC 외 anonymous direct GET은 거부한다.

리전 간 output은 확인한 parameter로 전달한다. 같은 리전 전용 `Fn::ImportValue`에 의존하지 않는다.

### 7.3 RDS

- 배포 당일 지원되는 MySQL engine version 확인
- local Docker MySQL과 migration 호환 검증
- private, encrypted, deletion protection
- backup 보존 기간과 maintenance window 명시
- 최초에는 기존 local data를 넣지 않음
- DB TLS `sslMode=VERIFY_IDENTITY`
- JDBC hostname은 인증서 검증 대상인 실제 RDS endpoint를 사용한다.
- 공식 `https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem`을 검증한 truststore로 사용한다.

CA와 JVM 연결 절차:

1. public CA bundle을 공식 HTTPS endpoint에서 받되, 검토·기록한 SHA-256과 함께 pin한다. hash가 달라지면 자동 수용하지 않고 AWS CA 변경 여부를 확인한다.
2. Docker builder stage에서 AWS 공식 예시처럼 bundle의 모든 인증서를 분리해 `keytool`로 PKCS12 truststore를 만들고 `keytool -list`로 내용과 만료일을 검증한다.
3. runtime image에 `/opt/app/certs/rds-truststore.p12`를 복사하고 `JAVA_TOOL_OPTIONS`의 `javax.net.ssl.trustStore*` 옵션으로 명시적으로 연결한다.
4. Connector/J URL은 처음부터 `sslMode=VERIFY_IDENTITY`를 유지한다. 연결 실패를 숨기기 위해 `VERIFY_CA`나 `REQUIRED`로 자동 강등하지 않는다.
5. CA rotation 시 bundle/hash/image를 갱신하고 새 image로 연결 검증한 뒤 RDS 인증서를 교체한다.

로컬에서는 private RDS에 직접 붙을 수 없으므로 RDS truststore 무결성을 확인하고, 별도 test CA를 쓴 TLS-enabled disposable MySQL로 Connector/J의 `VERIFY_IDENTITY` 동작까지 연습한다. 실제 RDS CA/hostname/TLS/Flyway 검증은 추가 bastion을 만들지 않고 같은 public application subnet·Security Group·Task Definition과 `assignPublicIp=ENABLED`를 쓰는 one-off ECS preflight task에서 수행한다.

최초 Flyway를 위해 RDS managed master credential을 사용하는 것은 초기 배포 타협이다. 실제 사용자 데이터 이전 전 application 계정과 migration 계정 분리를 검토한다.

### 7.4 ECS 초기 후보

```text
InitialDesiredCount=0
DesiredCountAfterPreflight=1
CPU=0.5 vCPU
Memory=1GB
ContainerPort=8080
HealthCheckPath=/actuator/health/readiness
HealthCheckGracePeriodSeconds=180
CloudWatchRetention=14 days
ALBIdleTimeout=120 seconds candidate
```

Docker test 결과에 따라 memory를 조정하고 COST GATE를 갱신한다.

Task 환경변수:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/ABC?sslMode=VERIFY_IDENTITY&serverTimezone=Asia/Seoul
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError <truststore options>
EPUB_BUCKET=<foundation output>
ASSET_BUCKET=<foundation output>
ASSET_BASE_URL=https://assets.portfoliodev.click
EPUB_PRESIGN_TTL_SECONDS=300
CORS_ALLOWED_ORIGINS=<DEPLOYED_FRONTEND_HTTPS_ORIGINS>
BOOK_API_ENABLED=false
```

DB/JWT/API secret은 Secrets Manager에서 필요한 field만 ECS로 주입한다. React가 아직 없으면 운영 CORS에 apex/`www`를 미리 넣지 않는다.

preflight mode는 같은 image와 JDBC/Flyway/JPA 설정으로 다음을 수행하고 성공 시 exit code 0으로 종료한다.

1. RDS endpoint의 `VERIFY_IDENTITY` 연결 성공
2. session의 `Ssl_cipher`가 비어 있지 않음을 확인
3. Flyway `V1/V2/V3` 적용 및 validate
4. JPA schema validate
5. `member_grade` 4개와 `grade_id=1/씨앗` 확인

실패하면 ECS Service는 `DesiredCount=0`으로 유지하고 CloudWatch log만 조사한다. 성공 후 별도 Regional Change Set으로 `DesiredCount=1`을 승인·적용한다.

### 7.5 IAM

- Execution Role: ECR pull, log write, 시작에 필요한 secret read
- Task Role: `epubs/*`, `covers/*`의 필요한 S3 action만
- `s3:*`, bucket wildcard, IAM/Route 53/RDS 관리 권한 금지

## 8. COST GATE

다음 항목을 사용자에게 보여주고 명시적 승인을 받은 후에만 Change Set을 실행한다.

- RDS engine/class/storage/backup/Multi-AZ 여부
- ECS CPU/memory/task 수
- ALB와 public IPv4 구성
- NAT Gateway 없음
- 서울 S3 bucket 2개
- CloudFront와 양 리전 ACM 인증서
- `us-east-1` 인증서 SAN `portfoliodev.click`, `*.portfoliodev.click` 및 DNS validation record
- 추가할 Route 53 record 전체
- IAM action/resource 범위
- Retain/deletion protection
- AWS Pricing Calculator 결과
- 세 stack Change Set의 Add/Modify/Remove/Replacement

비용 발생 가능 항목:

- Fargate, ALB/LCU, RDS/storage/backup
- public IPv4
- ECR/S3/CloudFront
- Secrets Manager, CloudWatch Logs, Route 53 query
- RDS가 생성된 뒤 서비스가 `DesiredCount=0`이어도 RDS/storage 비용은 발생하며, one-off preflight Fargate task에도 실행 시간만큼 비용이 발생한다.

S3 Gateway Endpoint와 AWS 통합 서비스용 non-exportable ACM public certificate에는 별도 시간당 요금이 없지만 연결된 서비스 사용료는 발생할 수 있다. Budget 알림은 리소스를 자동 중지하지 않는다.

승인 전에는 ECR/S3/RDS/ECS/ALB/CloudFront/ACM/Secrets/DNS를 생성·변경하지 않는다.

## 9. 배포 순서

1. 세 template validation과 lint
2. Foundation Change Set 검토·승인·실행
3. Docker image를 Git SHA tag로 ECR push하고 digest 기록
4. Edge Change Set 검토·승인·실행
5. exact Distribution ARN 조건을 사용한 Foundation OAC policy update 승인·실행
6. `DesiredCount=0`인 Regional Change Set 검토·승인·실행
7. one-off ECS preflight task로 RDS TLS/Flyway/JPA/reference data를 검증하고 log/exit code 확인
8. Regional `DesiredCount=1` update Change Set 검토·승인·실행
9. health, log, HTTPS, S3 권한 검증

각 Change Set은 다음 순서를 지킨다.

```text
create -> CREATE_COMPLETE 대기 -> 변경 상세 검토
-> 비용/DNS/IAM 설명 -> 사용자 승인 -> execute
```

Foundation update에서 bucket replacement가 보이거나 Regional Change Set에서 기존 리소스 Modify/Remove가 보이면 실행하지 않는다.

one-off preflight 정상 흐름:

```text
RDS VERIFY_IDENTITY -> TLS cipher 확인 -> Flyway V1/V2/V3
-> JPA validate -> reference data 확인 -> exit 0
```

preflight가 실패하면 `DesiredCount=0`을 유지해 circuit-breaker 재시작 loop를 만들지 않는다. 원인과 CloudWatch log를 보고하고, migration을 손으로 수정하거나 RDS TLS 검증을 약화하지 않는다. 성공 후 서비스가 시작되어야 `readiness UP -> ALB healthy`를 확인한다.

## 10. 배포 후 검증

### AWS와 log

- 모든 stack 완료
- ECS desired/running 일치
- Target Group healthy, 반복 재시작 없음
- Git SHA image/digest 일치
- Flyway/JPA/application startup log 정상
- preflight exit code 0, non-empty TLS cipher, `member_grade` 검증 정상
- prod profile, RDS/S3 region 정상
- log에 secret과 Presigned URL 없음

### HTTPS

```powershell
curl.exe -I http://api.portfoliodev.click/actuator/health/readiness
curl.exe -I https://api.portfoliodev.click/actuator/health/readiness
curl.exe -I https://assets.portfoliodev.click/<known-thumbnail-key>
```

- API HTTP는 HTTPS redirect
- API HTTPS 200과 유효한 인증서
- CloudFront thumbnail public 200
- S3 direct anonymous URL 403
- RDS와 ECS 8080은 인터넷에서 직접 접근 불가

### EPUB

| 경우 | 결과 |
|---|---|
| 비로그인 | 401 |
| 타인 rental | 403 |
| 만료 대여 | 거부 |
| 유효 대여/구매 | 짧은 URL 발급 |
| `UPLOADING/FAILED` | 조회 제외 |
| URL 만료 후 | S3 거부 |
| bucket 직접 접근 | 403 |

30MB와 50MB 근접 파일, 50MB 초과 파일을 확인하고 Range/재시도도 시험한다.

### 썸네일

- 관리자만 upload 가능
- CloudFront public 200와 올바른 Content-Type/cache header
- 위장 파일 거부
- S3 direct anonymous 403
- 기존 외부 cover URL 유지

## 11. 롤백과 후속 작업

롤백:

- 이전 Git SHA image와 Task Definition revision 유지
- ECS circuit breaker 자동 rollback
- 최초 DB migration은 additive만
- RDS deletion protection/backup
- S3 Versioning/Retain
- destructive SQL, bucket/DNS/stack 삭제는 별도 승인

최초 배포 후 별도 작업:

- local DB를 RDS로 이전하고 row count/FK/금액/checksum 검증
- 기존 DB EPUB을 S3로 옮긴 뒤 관찰 기간 운영
- 별도 승인 후에만 `epub_data` 정리
- React 저장소에서 API/assets URL, CORS, apex/`www` HTTPS 연결

## 12. 예상 변경 파일과 종료 보고

예상 파일:

```text
build.gradle
gradle.properties
.env.example
application*.properties
src/test/resources/application-test.properties
Testcontainers 공용 설정/meta-annotation과 32개 DB test
src/main/resources/db/migration/*
EPUB/S3/thumbnail/security 관련 Java와 test
RDS TLS preflight 관련 Java와 test
Dockerfile
.dockerignore
compose.yaml
docker/rds/global-bundle.pem
docker/rds/global-bundle.sha256
docker/rds/create-truststore.sh
infra/foundation.yml
infra/edge.yml
infra/regional.yml
infra/parameters.example.json
docs/AWS_배포_결과.md
```

`docs/AWS_배포_결과.md`에 기록:

- 수행한 명령어와 결과
- 변경 파일과 목적
- test/bootJar/Docker 결과
- 생성 리소스, 리전, 사양, 예상 비용
- Change Set과 배포 상태
- health/log/HTTPS/S3/EPUB 검증 결과
- 롤백 지점과 남은 후속 작업

기록 금지:

- secret, 계정 ID, 이메일/SSO URL, 전체 ARN
- ECR token, JWT, 전체 Presigned URL

## 13. 중단 조건

다음 중 하나면 중단하고 상태·영향·안전한 대안을 보고한다.

- 사용자 변경과 충돌하거나 secret이 노출됨
- 기준 test, migration, JPA validate 실패 원인 불명
- destructive migration 포함
- Docker image에 secret 또는 root 실행
- 기존 AWS 리소스/DNS 충돌
- 승인안과 비용/사양이 달라짐
- Change Set의 의도하지 않은 Replacement/Remove
- S3 public access 또는 RDS public access
- IAM wildcard 과다
- 인증서 리전 오류
- ECS 반복 재시작/readiness 실패
- EPUB 권한 우회, URL log 노출, S3 직접 접근 성공

## 14. 실제 시작 순서

사용자가 선호한 방식대로 한 번에 약 세 단계씩 진행한다.

1. CLI/Docker/JDK 확인 → Git/secret 확인 → test 미실행 compile 기준선
2. Testcontainers 격리 → 기존 schema 기준 32개 test → Flyway 전환 후 32개 test
3. EPUB/thumbnail S3 구현 → 전체 test/bootJar → Docker/RDS truststore 검증
4. AWS SSO → 읽기 전용 inventory → template/Change Set
5. 비용/DNS/IAM 설명 → 사용자 승인 → 인프라 + one-off preflight
6. 서비스 scale 승인·배포 → health/log/HTTPS/EPUB 권한 → 종료 보고

## 15. 공식 참고 문서

- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Spring Boot Testcontainers와 Service Connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Testcontainers MySQL](https://java.testcontainers.org/modules/databases/mysql/)
- [S3 Gateway Endpoint](https://docs.aws.amazon.com/vpc/latest/privatelink/vpc-endpoints-s3.html)
- [CloudFront OAC](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html)
- [CloudFront S3 Origin](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/DownloadDistS3AndCustomOrigins.html)
- [CloudFront Certificate Requirements](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/cnames-and-https-requirements.html)
- [ECS Fargate Networking](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/fargate-task-networking.html)
- [CloudFormation ECS Service](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-ecs-service.html)
- [S3 Presigned URL](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)
- [Amazon RDS SSL/TLS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL.html)
- [Amazon RDS CA truststore 생성과 rotation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL-certificate-rotation.html)
- [Java 17 MaxRAMPercentage](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html)
- [CloudFormation ImportValue](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/intrinsic-function-reference-importvalue.html)
