# ABC 배포 Runbook (EC2 Docker + private RDS + private S3 + CloudFront)

> 대상: `api.portfoliodev.click` (EC2 host Nginx + Let's Encrypt), `assets.portfoliodev.click` (CloudFront + us-east-1 ACM + OAC)
> 범위: 이 문서는 **코드 산출물 실행 절차**다. AWS resource 생성/변경 자체는 별도 작업이다.

## 0. 이 문서가 전제하는 것

| 항목 | 결정 |
|---|---|
| Application | EC2에서 Spring Boot Docker container 실행 |
| Database | private subnet RDS MySQL. Flyway 사용 안 함 |
| EPUB 신규 upload | private S3 |
| EPUB 기존 데이터 | DB LONGBLOB fallback 유지 |
| Cover | 기존 `coverImageUrl` 계약 유지 |
| Viewer API | 기존 streaming endpoint 그대로 |
| JPA | prod `ddl-auto=validate` |
| SQL init | prod `spring.sql.init.mode=never` |
| **S3 bucket versioning** | **필수. 꺼져 있으면 배포하지 않는다 (§1-1)** |
| Container healthcheck | application liveness 전용. DB health와 분리 (§4-2) |
| Credential | EC2 Instance Profile. access key/secret key property 없음 |

Actuator, ALB, ECS/Fargate, Flyway, Presigned URL API는 사용하지 않는다.

### compose 명령 형식

모든 compose 명령은 **반드시** 다음 형식을 쓴다.

```bash
docker compose --env-file .env.prod -f compose.prod.yml <command>
```

`--env-file`을 빼면 `${ABC_API_IMAGE}` / `${ABC_API_TAG}` 치환이 기본값으로 떨어져
의도하지 않은 image가 배포된다. 이 회귀는 다음으로 검증한다.

```bash
sh deploy/verify-compose-image-tag.sh
```

---

## 1. 사전 준비 (AWS 측, 이 저장소 범위 밖)

1. private subnet RDS MySQL 8.0.x, EC2 Security Group만 3306 inbound 허용
2. private S3 bucket (public access block 전체 ON, **versioning Enabled**)
3. EC2 Instance Profile 최소 권한
   - `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on `arn:aws:s3:::<bucket>/epubs/*`
   - `s3:GetObjectVersion`, `s3:DeleteObjectVersion` on `arn:aws:s3:::<bucket>/epubs/*`
   - `s3:GetBucketVersioning` on `arn:aws:s3:::<bucket>` (versioning preflight용)
   - migration reconciliation을 하려면 `s3:ListBucket` on `arn:aws:s3:::<bucket>` 추가
4. `api.portfoliodev.click` A record -> EC2 Elastic IP
5. EC2 Security Group inbound 80/443만 공개, 8080은 공개하지 않음

### 1-1. bucket versioning은 선택이 아니라 필수 조건

versioning이 꺼져 있으면 다음이 깨진다.

- 보상 삭제가 **version 단위가 아니라 key 단위**로 동작한다. 같은 도서/버전/내용을 재업로드하면
  key가 동일(`epubs/{bookId}/{epubVersion}/{sha256}.epub`)하므로, 실패한 transaction의 보상 삭제가
  **성공한 다른 transaction의 object까지 지울 수 있다.**
- migration 중 DB update 실패 시 보상 삭제가 복구 불가능해진다.

따라서 `EpubS3MigrationService`는 versioning이 꺼져 있으면 dry-run에서도 시작하지 않는다.
배포 preflight(§1-2)에서도 반드시 확인한다.

### 1-2. 배포 전 S3 smoke (bucket / region / Instance Profile 권한 검증)

**EC2 인스턴스 위에서** 실행한다. application을 띄우기 전에 수행해 IAM·bucket·region 문제를
기동 실패가 아니라 이 단계에서 잡는다.

```bash
BUCKET=<EPUB_BUCKET 값>
REGION=ap-northeast-2
KEY="epubs/_smoke/$(date +%s).epub"
```

```bash
aws sts get-caller-identity --query Arn --output text
```

출력이 `arn:aws:sts::<account>:assumed-role/<instance-role>/i-xxxxxxxx` 형태여야 한다.
`arn:aws:iam::<account>:user/...`가 나오면 access key가 섞여 있는 것이므로 중단한다.

```bash
aws s3api get-bucket-location --bucket "$BUCKET" --output text
```

`AWS_REGION`과 일치해야 한다 (`ap-northeast-2`는 `ap-northeast-2`로 출력).

```bash
aws s3api get-bucket-versioning --bucket "$BUCKET" --query Status --output text
```

반드시 `Enabled`. `None`이나 `Suspended`면 배포하지 않는다.

```bash
aws s3api get-public-access-block --bucket "$BUCKET" \
  --query PublicAccessBlockConfiguration --output json
```

4개 항목이 모두 `true`여야 한다.

**Put -> Get -> Delete 왕복 검증:**

```bash
head -c 1024 /dev/urandom > /tmp/abc-s3-smoke.bin
```

```bash
aws s3api put-object --bucket "$BUCKET" --key "$KEY" \
  --body /tmp/abc-s3-smoke.bin --content-type application/epub+zip \
  --query VersionId --output text
```

출력된 VersionId를 `VID`에 넣는다. `null`이 나오면 versioning이 꺼진 것이므로 중단한다.

```bash
aws s3api get-object --bucket "$BUCKET" --key "$KEY" --version-id "$VID" /tmp/abc-s3-smoke.out
```

```bash
cmp /tmp/abc-s3-smoke.bin /tmp/abc-s3-smoke.out && echo "S3 GET OK"
```

```bash
aws s3api delete-object --bucket "$BUCKET" --key "$KEY" --version-id "$VID"
```

```bash
aws s3api list-object-versions --bucket "$BUCKET" --prefix "$KEY" \
  --query '[Versions, DeleteMarkers]' --output json
```

둘 다 `null`이어야 정리 완료다. 남아 있으면 delete marker를 마저 지운다.

```bash
rm -f /tmp/abc-s3-smoke.bin /tmp/abc-s3-smoke.out
```

이 절차가 전부 통과해야 bucket 이름, region, Instance Profile 권한, versioning이 모두 정상이다.

---

## 2. DB 적용 순서 (수동, 반드시 코드 배포 전)

작업 전 [`docs/데이터베이스/README.md`](../docs/데이터베이스/README.md)를 완전히 읽는다.
read-only 상태 판별 없이 SQL을 실행하지 않는다.

### 2-1. RDS로 옮길 대상

로컬 development/demo DB의 **전체 dump**를 import한다.
automated test schema나 disposable schema는 import하지 않는다.

### 2-2. 순서

```text
1. 로컬 development/demo DB 전체 dump 생성 + 복원 가능성 확인
2. RDS에 dump import
3. RDS에서 docs/데이터베이스/README.md §4 상태 확인 SQL 실행 (read-only)
4. 상태가 MIXED/UNKNOWN이면 중단하고 차이를 먼저 보고
5. RDS backup snapshot 생성
6. V003__epub_s3_object_storage.sql 적용
7. V003을 한 번 더 실행해 idempotency 확인 (2회 실행 모두 exit 0)
8. 신규 코드 배포
```

### 2-3. V003 적용

V003은 `DELIMITER`를 사용하는 stored procedure 기반이므로 **반드시 `mysql` CLI로 실행**한다.
GUI client는 `DELIMITER`를 처리하지 못할 수 있다.

```bash
mysql -h <rds-endpoint> -u <user> -p <schema> < src/main/resources/db/manual/V003__epub_s3_object_storage.sql
```

```bash
echo "exit code: $?"
```

**exit code가 0이 아니면 구조가 기대와 다른 것이다. 추측으로 수정하지 말고 중단한다.**

V003은 스스로 다음을 강제한다. 하나라도 어긋나면 `SIGNAL SQLSTATE '45000'`으로 즉시 중단하며
어떤 DDL도 적용하지 않는다.

| 검사 | 실패 시 메시지 |
|---|---|
| `book_epub` 존재 | `book_epub table not found. Apply V001 first.` |
| 기존 9개 column의 정확한 type/nullability/default | `base column type/nullability/default mismatch.` |
| 신규 column이 0개 또는 3개 (부분 적용 금지) | `partial S3 columns present. Expected 0 or 3. MIXED state.` |
| 이미 있는 신규 column의 정확한 구조 | `existing S3 columns have wrong type/nullability/default.` |
| `uk_book_epub_book_version` = UNIQUE(book_id, epub_version) | `uk_book_epub_book_version must be UNIQUE(...)` |
| `uk_book_epub_storage_key` = UNIQUE(storage_key) 단일 column | `uk_book_epub_storage_key is not UNIQUE(storage_key).` |
| postflight 최종 column 계약 | `postflight failed: final book_epub column contract mismatch.` |
| postflight index 존재 | `postflight failed: uk_book_epub_storage_key missing or wrong columns.` |
| postflight row invariant | `postflight failed: book_epub rows violate storage invariants.` |

postflight도 procedure 안에서 SIGNAL을 던지므로, **기대 구조가 아니면 exit 0이 나오지 않는다.**

최종 계약:

| 항목 | 기대 |
|---|---|
| `epub_data` | `longblob`, `IS_NULLABLE=YES` |
| `storage_type` | `varchar(16)`, `NOT NULL`, default `'DB'` |
| `storage_key` | `varchar(512)`, nullable |
| `s3_version_id` | `varchar(128)`, nullable |
| `uk_book_epub_storage_key` | UNIQUE, column `storage_key` |
| 기존 row | 전부 `storage_type='DB'`, `epub_data` 그대로 유지 |

row invariant (application과 동일한 계약):

```text
DB row: storage_type='DB', epub_data NOT NULL, storage_key NULL
S3 row: storage_type='S3', storage_key와 s3_version_id 모두 non-blank (공백 문자열도 위반)
storage_type은 'DB' 또는 'S3'만 허용 (unknown 값 금지)
```

V003은 **모든 DDL 이전에** 다음 data preflight를 수행한다. 하나라도 위반하면
`ALTER TABLE`을 단 한 번도 실행하지 않고 중단한다.

| 상태 | preflight | 실패 시 메시지 |
|---|---|---|
| 신규 column 0개 | `epub_data IS NULL` row 존재 여부 | `epub_data IS NULL rows exist before adding S3 columns.` |
| 신규 column 3개 | 전체 row invariant (위 계약) | `existing rows violate storage invariants (pre-DDL).` |
| 신규 column 3개 | `storage_key` 중복 (UNIQUE index 생성 가능 여부) | `duplicate storage_key values block UNIQUE index creation.` |

> V003에는 자동 rollback SQL(U003)이 없다. 되돌리려면 2-2의 5단계 snapshot/dump를 복원한다.

---

## 3. 환경변수

```bash
cp .env.prod.example .env.prod
```

```bash
chmod 600 .env.prod
```

실제 값을 입력한다. AWS access key / secret key는 넣지 않는다.

prod에서 **없으면 기동이 실패하는** 변수:

| 변수 | 용도 |
|---|---|
| `DB_URL` | private RDS JDBC URL (`sslMode=REQUIRED` 포함) |
| `DB_USERNAME` | RDS 계정 (root 사용 금지) |
| `DB_PASSWORD` | RDS 비밀번호 |
| `JWT_SECRET` | 32자 이상. 로컬 개발용 값 재사용 금지 |
| `CORS_ALLOWED_ORIGINS` | frontend origin |
| `EPUB_STORAGE` | 반드시 `s3` |
| `AWS_REGION` | 예: `ap-northeast-2` |
| `EPUB_BUCKET` | private S3 bucket 이름 |

`EpubStorageProdGuard`가 `BeanFactoryPostProcessor`로 동작해 DataSource보다 먼저 검증하므로,
저장소 설정 오류는 DB 연결 실패에 가려지지 않고 그대로 드러난다.

기본값이 있는 변수: `JWT_EXPIRATION`, `BOOK_API_ENABLED`(기본 false),
`LIBRARY_TRUSTED_PROXIES`(기본 127.0.0.1), `EPUB_MIGRATION_*`(기본 비활성/dry-run),
`ABC_API_IMAGE`/`ABC_API_TAG`(기본 `abc-api:local`).

### `sslMode=REQUIRED`에 대한 명시적 tradeoff

fast path JDBC URL은 `sslMode=REQUIRED`를 사용한다.
이는 **전송 암호화만 보장하고 server hostname identity 검증은 하지 않는다.**
시연용으로 의도적으로 선택한 tradeoff이며, 다음 두 가지가 **필수 전제**다.

- RDS가 private subnet에 있고 public accessibility가 꺼져 있을 것
- RDS Security Group inbound 3306이 EC2 Security Group source로만 제한될 것

두 전제 중 하나라도 깨지면 `sslMode=VERIFY_IDENTITY` + RDS CA truststore로 올려야 한다.

---

## 4. 빌드와 실행

```bash
docker build -t abc-api:local .
```

### 4-1. compose 검증 — secret을 출력하지 않는 방법

`docker compose config`는 **`.env.prod`의 DB password와 JWT secret을 평문으로 그대로 출력한다.**
따라서 검증에는 `--quiet`를 쓴다. 문법/치환 오류가 있으면 exit code만으로 알 수 있다.

```bash
docker compose --env-file .env.prod -f compose.prod.yml config --quiet
```

```bash
echo "exit code: $?"
```

image tag만 눈으로 확인하려면 secret이 섞이지 않는 `--images`를 쓴다.

```bash
docker compose --env-file .env.prod -f compose.prod.yml config --images
```

**금지 사항:**

- `docker compose ... config`를 `--quiet` 없이 실행해 화면·로그·CI 출력에 남기지 않는다.
- `docker compose ... config > compose.resolved.yml` 처럼 파일로 저장하지 않는다.
  저장했다면 즉시 삭제하고 `JWT_SECRET`과 DB 비밀번호를 교체한다.
- 화면 공유나 이슈 첨부에 `config` 출력을 붙여넣지 않는다.
- 필요한 정보는 `--images`, `--services`, `--volumes`로만 확인한다.

### 4-2. 기동

```bash
docker compose --env-file .env.prod -f compose.prod.yml up -d
```

```bash
docker compose --env-file .env.prod -f compose.prod.yml ps
```

### healthcheck는 DB health와 분리돼 있다

container healthcheck는 **application liveness만** 본다.
인증이 필요한 `/`를 호출해 401 응답이 오면 살아있는 것으로 판정하며 DB를 건드리지 않는다.

```bash
docker inspect --format '{{.State.Health.Status}}' abc-api    # healthy 기대
```

DB 장애 때 container를 restart해도 문제가 해결되지 않고 오히려 재시작 루프에 빠지므로,
DB 상태는 healthcheck에 넣지 않는다.

**DB readiness는 배포 절차의 별도 smoke check로 확인한다.**

```bash
curl -fsS http://127.0.0.1:8080/api/v1/categories
```

```bash
curl -fsS https://api.portfoliodev.click/api/v1/categories
```

이 endpoint는 DB를 조회하므로 성공하면 RDS 연결과 schema까지 정상이라는 뜻이다.

### 4-3. application end-to-end smoke (EPUB 왕복)

§1-2가 AWS 자원 자체를 검증한다면, 여기서는 **application을 통해** upload -> DB metadata -> viewer
다운로드까지 실제로 왕복시킨다. `EPUB_STORAGE=s3` 경로가 정말로 동작하는지 확인하는 유일한 절차다.

**전제:** 관리자 계정, `AVAILABLE` 상태이며 `rental_type='FREE'`인 시연용 도서 1건(`bookId`),
유효한 EPUB 파일.

`FREE`가 아니면 (5)의 `POST /api/v1/rentals/free`가 실패하고, `AVAILABLE`이 아니면 대여 자체가 막힌다.

```sql
SELECT book_id, title, status, rental_type
FROM book
WHERE status = 'AVAILABLE' AND rental_type = 'FREE'
LIMIT 5;
```

```bash
API=https://api.portfoliodev.click
BOOK_ID=<시연용 bookId>
EPUB=/tmp/smoke.epub
```

#### (1) 관리자 로그인

```bash
ADMIN_TOKEN=$(curl -fsS -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' -d '{"loginId":"<admin-id>","password":"<admin-pw>"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
```

> 이 명령에는 관리자 비밀번호가 들어간다. shell history에 남기지 않으려면 명령 앞에 공백을 두거나
> `read -rs`로 입력받는다. 결과 token도 로그에 남기지 않는다.

#### (2) 원본 SHA-256 기록

```bash
sha256sum "$EPUB" | awk '{print $1}' | tee /tmp/smoke-src.sha256
```

#### (3) 관리자 EPUB upload

```bash
curl -fsS -X PUT "$API/api/v1/admin/books/$BOOK_ID/epub" -H "Authorization: Bearer $ADMIN_TOKEN" -F "file=@$EPUB;type=application/epub+zip"
```

응답 `data`의 `epubVersion`, `fileSize`, `sha256`를 기록한다.
`sha256`가 (2)의 값과 같아야 한다. 이 단계가 실패하면 S3 권한이나 bucket 설정 문제다.

#### (4) `book_epub` metadata 확인 (RDS에서 read-only)

```sql
SELECT book_epub_id, epub_version, storage_type, storage_key, s3_version_id,
       file_size, sha256, (epub_data IS NULL) AS blob_is_null
FROM book_epub
WHERE book_id = <BOOK_ID>
ORDER BY epub_version DESC
LIMIT 1;
```

기대값:

| column | 기대 |
|---|---|
| `storage_type` | `S3` |
| `storage_key` | `epubs/<bookId>/<epubVersion>/<sha256>.epub` |
| `s3_version_id` | **NOT NULL, non-blank** (blank면 versioning 문제 — §1-1) |
| `blob_is_null` | `1` (신규 upload는 `epub_data` 미사용) |
| `sha256` / `file_size` | 원본과 일치 |
| `epub_version` | 직전 버전 + 1 |

#### (5) viewer API 다운로드

일반 회원으로 로그인하고 해당 도서를 대여한 뒤 `rentalId`를 얻는다.

```bash
USER_TOKEN=$(curl -fsS -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' -d '{"loginId":"<user-id>","password":"<user-pw>"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
```

```bash
RENTAL_ID=$(curl -fsS -X POST "$API/api/v1/rentals/free" -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' -d "{\"bookId\":$BOOK_ID}" | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["rentalId"])')
```

이미 대여 중이면 `GET /api/v1/me/rentals`에서 기존 `rentalId`를 가져온다.

```bash
curl -fsS -D /tmp/smoke-headers.txt -o /tmp/smoke-download.epub "$API/api/v1/rentals/$RENTAL_ID/epub" -H "Authorization: Bearer $USER_TOKEN"
```

#### (6) SHA-256 비교

```bash
sha256sum /tmp/smoke-download.epub | awk '{print $1}' > /tmp/smoke-dl.sha256
```

```bash
diff /tmp/smoke-src.sha256 /tmp/smoke-dl.sha256 && echo "E2E SHA-256 MATCH"
```

**여기서 일치해야 S3 저장 -> DB metadata -> S3 streaming 전 구간이 정상이다.**

응답 header도 확인한다. `ETag`가 `"<sha256>"`, `Content-Length`가 원본 크기와 같아야 한다.

```bash
grep -iE "etag|content-length|content-type" /tmp/smoke-headers.txt
```

```bash
rm -f /tmp/smoke-download.epub /tmp/smoke-src.sha256 /tmp/smoke-dl.sha256 /tmp/smoke-headers.txt
```

#### (7) 대용량 upload 제한 확인 (Nginx + Spring 경계)

application `max-file-size`는 50MB, Nginx `client_max_body_size`는 51m다.
**30MB 이상 파일로 두 경계를 모두 확인한다.** 이 단계를 건너뛰면 Nginx 413이 시연 중에 처음 드러난다.

30MB대 정상 파일은 성공해야 한다(Nginx를 통과하고 timeout에도 걸리지 않아야 한다).

```bash
curl -fsS -w '\nhttp=%{http_code} time=%{time_total}s\n' -X PUT "$API/api/v1/admin/books/$BOOK_ID/epub" -H "Authorization: Bearer $ADMIN_TOKEN" -F "file=@/tmp/large-30mb.epub;type=application/epub+zip"
```

50MB를 넘는 파일은 `413`으로 거부돼야 한다. 유효한 EPUB일 필요는 없다.

```bash
head -c 55000000 /dev/urandom > /tmp/too-large.epub
```

```bash
curl -s -o /dev/null -w 'http=%{http_code}\n' -X PUT "$API/api/v1/admin/books/$BOOK_ID/epub" -H "Authorization: Bearer $ADMIN_TOKEN" -F "file=@/tmp/too-large.epub;type=application/epub+zip"
```

| 결과 | 해석 |
|---|---|
| `413` | 정상. Nginx(51m) 또는 Spring(50MB)이 거부 |
| `502` / `504` | Nginx timeout 또는 proxy 설정 문제. `proxy_read_timeout` 확인 |
| `200` | **설정 오류.** 50MB 초과가 통과하면 안 된다 |

```bash
rm -f /tmp/too-large.epub
```

30MB 성공 / 55MB 413이 모두 확인되면 Nginx `client_max_body_size`, `proxy_request_buffering off`,
timeout 설정이 실제로 동작하는 것이다.

> smoke로 만든 EPUB 버전은 `book_epub`에 그대로 남는다. 시연 데이터가 아니라면
> 시연 전에 해당 도서에 최종 EPUB을 다시 upload해 최신 버전으로 덮는다.
> (신규 upload는 항상 `epub_version`을 올리므로 이전 버전 row와 S3 object가 함께 남는다.)

### 기동 실패 진단

| 증상 | 원인 |
|---|---|
| `Could not resolve placeholder 'DB_URL'` | `.env.prod` 누락 또는 `--env-file` 미지정 |
| `prod profile requires app.epub.storage(EPUB_STORAGE)=s3` | `EPUB_STORAGE` 값이 s3가 아님 |
| `prod profile requires app.s3.epub-bucket(EPUB_BUCKET)` | bucket 미설정 |
| `prod profile requires app.aws.region(AWS_REGION)` | region 미설정 |
| `Schema-validation: missing column [storage_type]` | V003 미적용 상태로 배포함 |
| healthy인데 `/api/v1/categories`가 500 | application은 살아있고 DB/schema 문제 |

---

## 5. Nginx

```bash
sudo cp deploy/nginx/abc-api.conf /etc/nginx/sites-available/abc-api.conf
```

```bash
sudo ln -sf /etc/nginx/sites-available/abc-api.conf /etc/nginx/sites-enabled/abc-api.conf
```

certbot 실행 전에는 443 server block을 주석 처리한 채로 80만 올린다.

```bash
sudo nginx -t
```

```bash
sudo systemctl reload nginx
```

```bash
sudo certbot --nginx -d api.portfoliodev.click
```

certbot 이후 다시 `sudo nginx -t`가 exit 0인지 확인하고 reload한다.

주요 값: `client_max_body_size 51m`, `proxy_read_timeout 180s`, `proxy_send_timeout 180s`,
`X-Forwarded-Proto` / `X-Forwarded-For` / `Host` 전달, HTTP -> HTTPS 301 redirect.

application 쪽은 `server.forward-headers-strategy=framework`로 이 header들을 신뢰한다.

---

## 6. EPUB S3 one-off migration (선택, 명시적 opt-in)

기존 DB LONGBLOB EPUB을 S3로 옮긴다. **application 기동 시 자동 실행되지 않는다.**
성공한 row도 시연 rollback을 위해 기존 `epub_data`를 그대로 남긴다.

### 6-1. preflight (dry-run에서도 동일하게 수행)

runner는 어떤 row도 건드리기 전에 세 가지를 확인하고, 하나라도 실패하면 즉시 중단한다.

| preflight | 실패 시 |
|---|---|
| **단일 실행 lock** — MySQL `GET_LOCK('abc_epub_s3_migration', 0)` | 두 번째 runner는 exit 1. 중복 실행 불가 |
| **전체 row invariant** — DB/S3 계약 위반 row 수 | 위반 건수를 `failed`로 세고 **아무것도 옮기지 않은 채** exit 1 |
| **bucket versioning Enabled** | exit 1 (§1-1) |

invariant 검사 대상은 후보 row가 아니라 **`book_epub` 전체 row**다.

```text
DB row: epub_data NOT NULL, storage_key NULL
S3 row: storage_key NOT NULL
storage_type은 DB 또는 S3만 허용
```

lock은 전용 connection 하나를 migration이 끝날 때까지 붙잡는 방식이라,
두 개의 runner process가 동시에 도는 상황 자체가 성립하지 않는다.

### 6-2. dry-run (DB/S3 변경 0건)

```bash
docker compose --env-file .env.prod -f compose.prod.yml run --rm -e EPUB_MIGRATION_ENABLED=true -e EPUB_MIGRATION_DRY_RUN=true --entrypoint sh abc-api -c 'java -Dloader.main=com.acorn.abc.book.service.EpubS3MigrationRunner -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher'
```

```bash
echo "exit code: $?"
```

출력은 `candidates / migrated / skipped / failed / invariantViolations` count만 남는다.
key, URL, credential은 로그에 남기지 않는다.

**dry-run이 exit 1이면 손상 row가 있다는 뜻이다.** `invariantViolations` 건수를 확인하고
DB를 먼저 정리한 뒤 다시 dry-run이 exit 0이 될 때까지 실제 실행을 하지 않는다.

### 6-3. 실제 실행

`EPUB_MIGRATION_DRY_RUN=false`로 같은 명령을 실행한다.

동작 보장:

- `storage_type='DB'` row만 처리한다.
- DB의 `file_size` / `sha256`를 신뢰하기 전에 실제 blob stream과 대조한다. 불일치 row는 건드리지 않고 `failed`로 센다.
- deterministic key `epubs/{bookId}/{epubVersion}/{sha256}.epub`를 사용한다.
- upload 실패 시 DB row를 변경하지 않는다.
- DB update 실패 시 방금 upload한 **exact object version**을 보상 삭제한다.
- 이미 `storage_type='S3'`인 row는 skip하므로 재실행해도 안전하다.
- `failed >= 1`이면 exit code가 non-zero다.

exit code: `0` 성공 / `1` failed 1건 이상, preflight 실패, 또는 예외 / `2` opt-in 누락.

> 실행 후 `EPUB_MIGRATION_ENABLED`를 다시 `false`로 되돌린다.

### 6-4. orphan object reconciliation

다음 두 경우에는 S3에 object가 남고 DB에는 대응 row가 없을 수 있다.

1. transaction 결과가 `STATUS_UNKNOWN`인 경우 — **의도적으로 삭제하지 않는다.**
   commit 여부를 모르는 상태에서 지우면 commit된 row의 실데이터를 날릴 수 있기 때문이다.
2. 보상 삭제 자체가 실패한 경우 — 원래 DB 예외를 가리지 않기 위해 suppressed 예외와 로그로만 남긴다.

두 경우 모두 application log에 다음이 남는다.

```text
EPUB S3 transaction outcome unknown, object left for reconciliation: key=..., versionId=...
EPUB S3 compensating delete failed after rollback, object is orphaned: key=..., versionId=...
```

reconciliation 절차 (수동, 승인 후):

```bash
aws s3api list-object-versions --bucket "$BUCKET" --prefix "epubs/" \
  --query 'Versions[].[Key,VersionId]' --output text > /tmp/s3-objects.txt
```

```sql
SELECT storage_key, s3_version_id FROM book_epub WHERE storage_type = 'S3';
```

두 목록을 비교해 **DB에 없는 (key, versionId)만** 삭제 후보로 삼는다.
DB에 있는 object는 절대 지우지 않는다. 삭제는 반드시 version 지정으로 수행한다.

```bash
aws s3api delete-object --bucket "$BUCKET" --key "<key>" --version-id "<versionId>"
```

---

## 7. Rollback

### 7-1. 코드만 되돌리는 경우 (중요)

**구버전 image만 복귀하면 신규 EPUB을 읽을 수 없다.**

신규 upload로 생긴 S3 row는 `epub_data = NULL`이다. 구버전 코드는 `storage_type`을 모르고
DB LONGBLOB만 읽으므로 해당 도서의 viewer 다운로드가 깨진다.

따라서 구버전 rollback은 다음 중 하나를 **함께** 수행해야 한다.

1. 배포 직전 DB snapshot / dump 복원 (신규 S3 row 자체가 사라짐), 또는
2. 신규 S3 row 제거 — `storage_type='S3' AND epub_data IS NULL` 인 `book_epub` row를 삭제
   (해당 도서는 EPUB 없음 상태가 되므로 재업로드 필요)

두 방법 모두 데이터 손실을 동반한다. 실행 전 반드시 승인과 backup을 확보한다.

### 7-2. migration runner가 옮긴 기존 row

runner가 옮긴 row는 `epub_data`를 그대로 유지한다. 따라서 metadata만 DB mode로 되돌리면
구버전 코드에서도 계속 읽을 수 있다.

다만 이 되돌리기는 **자동화하지 않았다.** 별도 승인 절차를 거쳐 수동으로 수행한다.
대상 판별 기준: `storage_type='S3' AND epub_data IS NOT NULL`.
되돌린 뒤에는 대응하는 S3 object 정리 여부를 §6-4 절차로 별도 결정한다.

### 7-3. DB 구조

V003은 additive이며 U003(자동 rollback SQL)을 제공하지 않는다.
구조를 되돌려야 하면 §2-2의 5단계 snapshot을 복원한다.

---

## 8. 알려진 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| 구버전 image 단독 rollback | 신규 EPUB 읽기 불가 | §7-1 |
| `sslMode=REQUIRED` | hostname 검증 없음 | private RDS + SG source 제한 필수 |
| **bucket versioning OFF** | 보상 삭제가 정상 object를 지울 수 있음 | **배포 필수 조건. §1-1, §1-2 preflight** |
| `STATUS_UNKNOWN` transaction | orphan object 잔존 | 의도적 설계. §6-4 reconciliation |
| 보상 삭제 실패 | orphan object 잔존 | 원래 예외를 가리지 않고 로그 기록. §6-4 |
| S3 장애 | viewer 다운로드/업로드 500 | 기존 전역 500 처리와 동일. DB blob으로 조용히 fallback하지 않음 |
| unknown `storage_type` | 읽기 실패(500) | 의도적. DB fallback으로 손상 metadata를 숨기지 않음 |
| `--env-file` 누락 | 잘못된 image tag 배포 | `sh deploy/verify-compose-image-tag.sh` |
| `compose config` 출력 유출 | DB password / JWT secret 노출 | `--quiet` 사용. §4-1 |
| `.env.prod` 유출 | DB/JWT 노출 | `chmod 600`, commit 금지, `.gitignore`가 `.env.*` 무시 |
| V003 미적용 상태 배포 | `ddl-auto=validate` 실패로 기동 불가 | §2를 배포보다 먼저 수행 |
| migration 중복 실행 | 동일 row 경합 | `GET_LOCK` 단일 실행 guard. §6-1 |
