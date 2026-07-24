# ABC CI/CD 구축 계획 (canonical 설계 문서)

- 상태: APPROVE (설계 확정). 구현 착수 전.
- 기준일: 2026-07-24
- 현재 production 기준 commit: `a5b493c`
- 범위: backend(EC2 Docker) + frontend(S3 + CloudFront)의 CI/CD 파이프라인 설계
- 이 문서의 성격: 설계 canonical. 실제 운영 명령은 구현 완료 후 [`deploy/README.md`](../../deploy/README.md)에 반영한다.

## 관련 문서

| 문서 | 역할 |
|---|---|
| [`deploy/README.md`](../../deploy/README.md) | 현행 수동 배포 runbook. CI/CD 구현 후 운영 명령 반영 대상 |
| [`docs/데이터베이스/README.md`](../데이터베이스/README.md) | DB 상태 판별/schema bootstrap/migration 절차. CI DB 및 migration gate의 근거 |
| `.omc/plans/google-login-redeploy-release-plan.md` (local 전용, `.gitignore`로 제외 -- 링크 아님) | deploy/main 통합 + Google 로그인 backend-first release 순서 |
| `docs/codex최종계획.md` | 기존 수동 배포 당시 의사결정 근거(보존, 이 문서로 대체하지 않음) |

---

## 0. 배경과 현재 상태

| 구성 | 저장소 | 배포 위치 | 현재 방식 | CI/CD |
|---|---|---|---|---|
| backend | `shangpa/AcornProject` | `api.portfoliodev.click` (EC2 Docker + host Nginx + Let's Encrypt) | 수동 | 없음 |
| frontend | `rlarjsgml1/ABCProjectFront` | `portfoliodev.click` (S3 + CloudFront, `Server: AmazonS3` 확인) | 수동 | 없음 |

확인된 제약:

- `deploy` branch가 local 전용이고 origin에 없다. 배포 구성(Dockerfile, compose.prod.yml, deploy/)이 GitHub에 백업되지 않는다.
- `deploy`와 `origin/main`이 diverged. main이 deploy보다 7 commits 앞, deploy가 main보다 3 commits 앞. 즉 현재 production은 main의 7 commits가 빠진 tree를 돌린다.
- backend image는 [`compose.prod.yml:22`](../../compose.prod.yml)의 `${ABC_API_IMAGE}:${ABC_API_TAG}` interpolation으로 결정된다. 단순 `pull + up`으로는 새 image가 배포되지 않는다.
- 신규 S3 EPUB row는 `epub_data=NULL`이라, 구버전 image 단독 rollback은 해당 도서 viewer를 깨뜨린다([`deploy/README.md:630`](../../deploy/README.md)).

---

## 1. 설계 방향

방향 A를 단계적으로 구축한다. 단순 "push하면 배포"가 아니라 CI gate + immutable artifact + production 수동 승인 + rollback 구성이다.

```text
PR / push
  -> CI 자동 검증 (test, build, image, secret scan, compose 검증, schema diff)
  -> immutable artifact 생성 (ECR image = git SHA tag / S3 releases/<sha>/)
  -> production 수동 승인 (GitHub Environment)
  -> CD 배포 (OIDC + SSM Run Command / S3 promote)
  -> health / readiness smoke
  -> 실패 시 정책에 따라 중단 또는 수동 rollback
```

핵심 원칙:

- CI는 PR/push마다 완전 자동. PR에서는 AWS 배포를 절대 하지 않는다.
- production CD는 `workflow_dispatch` 수동 실행 또는 release tag + GitHub production Environment 승인으로만 실행.
- artifact는 immutable. `latest` tag를 쓰지 않고 git commit SHA tag를 쓴다.
- AWS 인증은 장기 access key가 아니라 GitHub Actions OIDC 임시 credential.
- 1차 범위에서 backend 자동 rollback과 staging은 제외한다(3장, 8장).

---

## 2. Branch 정상화 (CI/CD 이전 선행)

CI/CD와 별개로 가장 먼저 처리한다. 상세 절차는 Google 계획 문서 Phase 0~1을 따른다.

1. 현재 production commit `a5b493c`와 running image digest를 rollback 기준점으로 보존한다. immutable Git tag(예: `prod-20260724-a5b493c`)를 만들어 remote에 push한다.
2. `origin/main`에서 `feature/deploy-sync`를 만들고 현재 `deploy`의 3 commits를 `--no-ff` merge한다.
3. review 후 merge commit 방식으로 main에 병합한다(squash/rebase 금지). deployment/S3 구성을 main에 먼저 반영한다.
4. 이후 main을 source of truth로 사용한다. `deploy`는 개발 branch가 아니라 release pointer로만 쓰거나 제거한다.

> deploy branch를 GitHub에 올리는 것 자체는 CI/CD가 아니라 단순 backup이다.

주의: 첫 baseline release는 배포 설정만이 아니라 main의 7 commits까지 함께 production에 올린다. 통합 전에 10 commits 전체 delta(특히 `SecurityConfig`, `JwtAuthenticationFilter`, `Member`, `build.gradle`, application/schema)를 검토한다.

---

## 3. Rollback 정책 (1차 확정)

1차 범위에서 backend 자동 rollback과 staging 조건을 모두 제거한다. YAGNI와 내부 모순(단일 EC2뿐이라 staging 검증 조건을 만족할 수 없음)을 제거하기 위함이다.

| 대상 | 1차 정책 |
|---|---|
| frontend | immutable `releases/<sha>/` prefix 기반 자동 복구 가능 |
| backend 일반 release | feature flag OFF 후 **수동** image rollback |
| DB | additive 변경 유지. 수동 판단 |
| backend 자동 rollback | staging 구축 후 **후속 phase** |

근거: 구버전 image 단독 rollback은 신규 S3 EPUB(`epub_data=NULL`)을 못 읽어 viewer가 깨진다([`deploy/README.md:630`](../../deploy/README.md)). 자동 rollback을 안전하게 하려면 staging에서 이전 image의 EPUB viewer 검증이 선행돼야 하는데, 1차 인프라에는 staging이 없다.

---

## 4. Backend CI

PR과 main push에서 자동 실행한다. AWS 배포는 하지 않는다.

1. `./gradlew test` (CI MySQL service 사용, 6장)
2. `./gradlew build`
3. Docker image build
4. secret 포함 여부 검사 (image + repo)
5. compose 설정 검사
   - placeholder `.env.prod.ci`(가짜 값)로 `docker compose --env-file .env.prod.ci -f compose.prod.yml config --quiet` 실행. 실 secret 없이 문법/치환 검증.
   - 기존 [`deploy/verify-compose-image-tag.sh`](../../deploy/verify-compose-image-tag.sh)를 그대로 wiring해 `--env-file` 누락 회귀를 검증.
6. schema 독립 검증 (7장)

구현 세부:

- Dockerfile은 multi-stage로 소스부터 bootJar를 재빌드한다(`-x test`). CI에서 gradle build를 따로 하면 이중 compile이므로, image build 하나로 통합하고 test만 별도로 두거나 jar를 COPY하는 방식을 검토한다. (비차단 최적화)

---

## 5. Backend CD

main push마다 바로 EC2에 배포하지 않는다. 다음 조건으로 제한한다.

- `workflow_dispatch` 수동 실행 또는 release tag
- GitHub production Environment 승인 + branch 제한
- Docker image는 `latest`가 아니라 commit SHA tag, ECR push
- AWS SSM Run Command로 EC2 배포 (SSH key를 GitHub에 넣지 않음)

### 5-1. SSM 배포 절차

```text
기존 .env.prod 보안 backup
  -> .env.prod의 ABC_API_IMAGE, ABC_API_TAG만 원자적 갱신 (temp write + mv)
  -> ECR login
  -> exact SHA image pull
  -> docker compose --env-file .env.prod -f compose.prod.yml config --quiet
  -> docker compose ... up -d
  -> health polling (5-2)
  -> readiness (5-2)
  -> 성공 시 release metadata 갱신 (9장)
  -> 실패 시 정책에 따라 중단 또는 승인형 rollback (3장)
```

- SSM에는 image URI, SHA tag, release ID만 전달한다.
- concurrency group으로 production 배포를 직렬화한다(`cancel-in-progress: false`).

### 5-2. health polling과 readiness

단일 container이므로 짧은 downtime을 수용한다. 무중단 배포는 1차 범위 제외.

[`compose.prod.yml:33`](../../compose.prod.yml)의 healthcheck `start_period`가 90초이므로 배포 직후 즉시 smoke하면 안 된다.

```text
docker compose up -d
  -> 5초 간격 health polling
  -> healthy: 다음 단계
  -> unhealthy/exited: 즉시 실패
  -> 300초 timeout: 실패
  -> GET /api/v1/categories readiness retry (DB 연결 + schema 확인)
```

- SSM command timeout은 300초보다 길게 잡는다.
- liveness/readiness 확인은 [`deploy/README.md:318`](../../deploy/README.md) 절차를 따른다.

### 5-3. SSM output 보안

SSM Run Command 출력은 SSM command history / CloudWatch에 영구 기록될 수 있다. 다음을 금지한다.

- `set -x`
- `.env.prod` 출력
- `env`, `printenv`
- `--quiet` 없는 `docker compose config`
- secret이 command argument에 포함되는 방식
- ECR password를 stdout으로 출력

결과 log에는 release SHA와 health 결과만 남긴다.

---

## 6. CI 테스트 DB

현재 구조 확인:

- `@DataJpaTest` 24개, `@SpringBootTest` 8개, 대부분 `AutoConfigureTestDatabase.Replace.NONE`(31개 파일)
- build.gradle에 H2/Testcontainers 의존성 없음, mysql은 `runtimeOnly`
- MySQL 전용 DDL, lock, constraint 존재

따라서 H2는 제외한다. Testcontainers 전환은 후속 개선. 1차는 MySQL 8.0.40 service container.

### 6-1. CI DB 환경 일치

CI database 생성 전에 production을 read-only로 조회해 값을 맞춘다(추측 고정 금지).

```sql
SELECT VERSION(), @@character_set_server, @@collation_server, @@sql_mode, @@time_zone;
```

CI가 맞출 대상: MySQL 8.0.40 / `utf8mb4` / `utf8mb4_0900_ai_ci` / production과 동일한 `sql_mode` / `Asia/Seoul` / JDBC `serverTimezone=Asia/Seoul`.

### 6-2. Fresh CI bootstrap (canonical)

신규 빈 DB에서 V001~V003 및 library migration을 **실행하지 않는다**. 최종 구조는 이미 bootstrap schema에 반영돼 있다([`docs/데이터베이스/README.md:239`](../데이터베이스/README.md), [`schema.sql:268`](../../src/main/resources/schema.sql), [`V003:5`](../../src/main/resources/db/manual/V003__epub_s3_object_storage.sql)).

```text
MySQL 8.0.40 service 실행
  -> abc_ci database 생성 (charset/collation 일치)
  -> schema.sql
  -> 관리자회원_계정정지연결및포인트멱등키.sql
  -> 관리자회원_포인트멱등응답스냅샷.sql
  -> 필요한 seed
  -> runtime-contract SQL
  -> ddl-auto=validate
  -> ./gradlew test
```

이 순서는 `deploy/db/bootstrap-manifest.txt` 같은 단일 manifest로 관리한다. workflow에 SQL 파일명을 직접 중복 기재하면 drift가 재발한다.

> 실행 금지 목록: `V001__epub_additive.sql`, `V002__request_notification_notice_alignment.sql`, `V003__epub_s3_object_storage.sql`, `0001_physical_library_location.sql`, `0002_unify_library_isbn.sql`.

### 6-3. Migration rehearsal (Regular CI와 분리)

```text
base SHA의 canonical schema
  -> head에서 추가된 migration 적용
  -> migration 재실행 / idempotency
  -> head SHA fresh bootstrap과 runtime contract 비교
```

- Google V004 같은 신규 migration은 head schema.sql(이미 V004 구조 포함)이 아니라 **base SHA schema**에 적용해야 의미가 있다.
- V003류는 `DELIMITER` + stored procedure + `SIGNAL`을 쓰므로 rehearsal은 반드시 `mysql` CLI로 실행한다(gradle sql-init/JDBC multi-statement 금지).

---

## 7. Release contract 독립 검증

`release-contract.yml`만 신뢰하지 않는다(개발자가 실제로 컬럼을 추가하면서 `schemaChange: none`으로 적을 수 있음). JPA 생성 schema만 비교하는 것도 부족하다(JPA mapping에 없는 index, CHECK, migration metadata 누락). 다음 3중 검증을 쓴다.

1. **Base/head MySQL schema diff**
   - base와 head를 각각 canonical bootstrap
   - normalized `mysqldump --no-data` 비교
   - 차이가 있는데 `schemaChange: none`이면 CI 실패
2. **Migration convergence**
   - base schema + head migration
   - head fresh bootstrap
   - migration 추적 table을 제외한 runtime contract가 같아야 함 (schema.sql이 migration과 어긋나지 않게 막는 guard)
3. **JPA validation**
   - head fresh DB에서 `ddl-auto=validate`
   - production에서는 동일한 read-only `INFORMATION_SCHEMA` contract SQL 실행

### 7-1. schema 비교 기준 (2개로 분리)

| 검증 | base | head |
|---|---|---|
| PR 검증 | target branch base | PR head |
| 배포 검증 | **현재 production SHA** | release SHA |

`deploy/main`이 갈라진 상태에서는 배포 검증(현재 production SHA 기준)이 반드시 필요하다. base를 merge-base로 쓰면 diverged 상황에서 의미가 없다.

### 7-2. 1차 release-contract.yml

```yaml
schemaChange: none
requiredMigrations: []
runtimeContractVersion: 1
epubContractChange: false
```

`rollbackEligible`은 1차에서 제거한다(3장: backend 자동 rollback 없음).

---

## 8. DB migration CD gate

DB migration은 "수동"만으로 부족하고, CD 전에 명시적 gate가 필요하다.

```text
release contract 확인
  -> RDS snapshot
  -> migration exit code 0
  -> INFORMATION_SCHEMA postflight
  -> ddl-auto=validate 확인
  -> production 승인
  -> application deploy
```

`requiredMigrations`가 비어 있지 않은데 migration evidence가 없으면 CD job이 시작되지 않도록 한다.

주의: CD smoke가 `/api/v1/categories`(DB 조회)를 치므로, release가 스키마 변경을 요구하는데 migration이 선행되지 않으면 `ddl-auto=validate` 기동 실패 -> smoke 실패로 이어진다. 이 gate가 그 순서 의존을 명시적으로 강제한다.

> RDS snapshot restore는 일반 rollback이 아니라 disaster recovery다. 새 instance 생성 + endpoint cutover + DB_URL 변경 가능성 + 수분~수십분 downtime + snapshot 이후 write 손실 가능. Google additive schema는 backend rollback 시 DB에서 제거하지 않고, snapshot restore는 별도 승인과 data-loss 판단이 있을 때만 쓴다.

---

## 9. Release metadata 표준

| 계층 | 저장소 |
|---|---|
| authoritative metadata | SSM Parameter Store |
| audit / UI mirror | GitHub Deployment |
| physical ground truth | running container image digest |

권장 Parameter: `/abc/prod/backend/current-release`

```json
{
  "gitSha": "...",
  "imageUri": "...",
  "imageDigest": "...",
  "deployedAt": "...",
  "workflowRunId": "..."
}
```

- 갱신 시점: health/readiness 성공 후.
- SSM 기록, GitHub Deployment, running digest가 다르면 **자동 진행하지 않고 reconciliation 대상으로 중단**한다.
- 배포 SHA 추적에 moving tag를 쓰지 않는다. immutable Git tag(`prod-YYYYMMDD-sha`) + 위 별도 기록 + `.env.prod`의 `ABC_API_TAG`와 실제 container image digest까지 교차 검증한다.

---

## 10. Frontend CI/CD

별도 repository `rlarjsgml1/ABCProjectFront`에 구축한다.

### 10-1. CI

- `npm ci`
- `npm run lint`
- `npm run build`
- production bundle에 `localhost`가 없는지 검사
- 필수 build variable 존재 검사: 현재는 `VITE_API_BASE_URL`, `VITE_NAVER_MAP_CLIENT_ID` 2개만. Google 구현 PR부터 `VITE_GOOGLE_CLIENT_ID` 추가.

> `VITE_NAVER_MAP_CLIENT_ID`, `VITE_GOOGLE_CLIENT_ID`는 frontend bundle에 공개되는 client ID다. GitHub Secret이 아니라 Environment variable로 관리하되 build 전 존재 여부를 검증한다. 실제 사용 위치: 별도 frontend repo `rlarjsgml1/ABCProjectFront`의 `src/components/common/NaverMap.tsx:113`.

### 10-2. CD

```text
dist/를 s3://.../releases/<git-sha>/에 업로드
  -> 승인 후 current/로 promote (S3 copy 방식. CloudFront origin path 변경 금지)
  -> CloudFront /* invalidation
  -> smoke: /, /login, deep link, API CORS
  -> 실패 시 이전 releases/<sha>/를 다시 promote
```

cache-control:

- hashed asset: immutable long cache
- `index.html`: `no-cache, no-store, must-revalidate` (promote 후 stale index가 옛 asset hash를 참조하지 않게)

> React Router deep link(`/login` 등)가 S3에서 직접 요청되면 403이므로, CloudFront custom error response(403/404 -> `/index.html`, 200)가 인프라 선행조건이다. smoke에서 실제 `/login`이 200인지 확인한다.

---

## 11. AWS 인증과 선행조건

GitHub Secrets에 장기 AWS access key를 저장하지 않는다. GitHub Actions OIDC + repository/environment로 제한된 IAM trust policy를 쓴다.

### 11-1. GitHub OIDC role (backend/frontend 분리)

- backend: ECR push + 대상 EC2에 한정된 SSM command
- frontend: frontend S3 prefix + CloudFront invalidation
- 두 repository의 정확한 owner/repo(`shangpa/AcornProject`, `rlarjsgml1/ABCProjectFront`는 서로 다른 owner) 및 `environment:production`으로 trust 제한

### 11-2. EC2 instance profile (SSM CD 필수조건)

현재 instance profile은 S3 권한 위주다. SSM CD를 위해 다음을 추가한다.

- SSM Agent 실행 및 managed instance 등록, `AmazonSSMManagedInstanceCore` 상당 권한
- ECR pull: `ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchCheckLayerAvailability`
- AWS endpoint outbound 443

> "현재 EC2에 권한이 없다"는 repository만으로 확정할 수 없다. 다만 위 권한은 SSM CD의 필수조건이므로 구현 전 실제 instance profile을 확인한다.

---

## 12. DB migration 자동화 제외

Google 로그인 DB migration은 production 자동 실행 대상에서 제외한다.

```text
read-only preflight
  -> RDS snapshot
  -> 수동 승인
  -> migration
  -> postflight
  -> application CD 승인
```

현재 프로젝트의 DB 상태 판별 및 MIXED/UNKNOWN 중단 규칙 때문에 이 부분을 main push에 연결하면 위험하다.

---

## 13. Phase 순서

| Phase | 내용 | 선행조건 |
|---|---|---|
| 0 | branch 정상화 (2장) | - |
| 1 | AWS 선행조건 (11장: OIDC role, EC2 instance profile, ECR repo, CloudFront error response) | 0 |
| 2 | backend CI (4장) + CI DB (6장) | 1 |
| 3 | release contract 독립 검증 (7장) | 2 |
| 4 | backend CD (5장) + release metadata (9장) + migration gate (8장) | 1, 2 |
| 5 | frontend CI/CD (10장) | 1 |
| 6 | baseline release (main 10 commits) 배포 및 관찰 | 4, 5 |
| 7 | Google 로그인 별도 release (Google 계획 문서) | 6 |

---

## 14. 1차 범위 제외 (후속 phase)

- backend 자동 rollback
- staging 환경 구축
- 무중단(zero-downtime) 배포
- Testcontainers 전환
- release-contract의 `rollbackEligible` 자동 판정

---

## 15. 주요 위험과 완화

| 위험 | 완화 |
|---|---|
| 구버전 image 단독 rollback으로 신규 EPUB 읽기 불가 | backend rollback은 수동 + feature flag OFF 선행 (3장, `deploy/README.md:630`) |
| CI/prod schema drift | fresh CI = schema.sql + sidecar 2개, V-migration 실행 금지. migration convergence로 지속 검증 (6장, 7장) |
| release-contract self-assertion | base/head schema diff로 독립 검증 (7장) |
| `--env-file` 누락으로 잘못된 image tag 배포 | `verify-compose-image-tag.sh` CI wiring (4장) |
| SSM/Actions log secret 유출 | output scrubbing, `config --quiet`, command argument에 secret 금지 (5-3) |
| deploy/main divergence | main을 source of truth로 통합, baseline release delta 검토 (2장) |
| 배포 SHA 추적 불일치 | SSM/GitHub Deployment/container digest 3자 교차 검증, 불일치 시 중단 (9장) |
| migration 미선행 배포 | requiredMigrations evidence 없으면 CD 미시작 (8장) |
| Google 교차기능 부분배포 | backend-first release + feature flag (Google 계획 Phase 5) |
