# Backend CI/CD 개발자 사용 가이드

## 1. 목적

이 문서는 `shangpa/AcornProject`에서 개발, review, production 배포를 수행하는
개발자를 위한 운영 가이드다.

상세 설계와 의사결정 근거는 같은 디렉터리의 `CI-CD_구축계획.md`를 참고한다.

현재 production 기준:

- Git SHA: `30d1c30332356e94dc20bfe3e418e1bff80f0099`
- Git tag: `prod-20260724-30d1c30`
- 배포 대상: EC2 Docker container `abc-api`
- artifact: ECR image `<repository>:<full-git-sha>`

## 2. 전체 흐름

```text
feature branch
  -> pull request to main
  -> Backend CI 자동 실행
  -> review 및 merge
  -> main Backend CI 재실행
  -> 운영자가 Backend Production CD 수동 실행
  -> GitHub OIDC로 AWS 임시 권한 획득
  -> ECR immutable image 생성
  -> SSM Run Command로 EC2 배포
  -> container health와 API 검증
  -> production Git tag와 release metadata 기록
```

`main`이 배포 source of truth다. `deploy` branch 또는 개발자 로컬 image를
production에 직접 배포하지 않는다.

## 3. Backend CI

Workflow:

- `.github/workflows/backend-ci.yml`
- 실행 조건: `main` 대상 pull request, `main` push

필수 check:

| Check                 | 검증 내용                                                    |
| --------------------- | ------------------------------------------------------------ |
| `Test (MySQL 8.0.40)` | canonical DB bootstrap, schema/runtime contract, Gradle test |
| `Test (MySQL 8.0.42)` | MySQL 호환 버전에서 동일 검증                                |
| `Docker and Compose`  | credential pattern 검사, Docker build, Compose 검증          |

CI의 DB 환경은 production 계약과 동일한 timezone, charset, collation,
sql mode를 사용한다. PR에서는 target base SHA와 head SHA의 schema를
독립적으로 비교한다.

CI가 실패하면 merge하거나 Production CD를 실행하지 않는다.

## 4. 일반 개발 절차

```bash
git switch main
git pull
git switch -c feature/<작업명>

./gradlew test

git push -u origin feature/<작업명>
```

이후 GitHub에서 `feature/* -> main` pull request를 생성한다.

PR 확인 항목:

1. 세 개의 Backend CI check가 모두 성공했는가
2. API response contract가 유지되는가
3. DB, EPUB storage, environment variable 변경이 있는가
4. production rollback 호환성에 영향이 있는가
5. `.env`, credential, private key가 commit되지 않았는가

## 5. DB 변경이 있는 개발

DB 작업 전 `docs/데이터베이스/README.md`를 완전히 읽는다.

금지 사항:

- `docs/데이터베이스/ddl/schema.sql`을 production DB에 직접 실행
- `MIXED/UNKNOWN` 상태 자동 수정
- test schema 또는 test data를 실제 DB에 병합
- migration 적용 증거 없이 Production CD 실행

DB 변경 PR에서는 `release-contract.yml`도 실제 영향에 맞게 갱신한다.

```yaml
schemaChange: none
requiredMigrations: []
runtimeContractVersion: 1
epubContractChange: false
```

필드 의미:

| 필드                     | 의미                                              |
| ------------------------ | ------------------------------------------------- |
| `schemaChange`           | schema 변경 영향                                  |
| `requiredMigrations`     | production 배포 전에 적용해야 하는 migration 목록 |
| `runtimeContractVersion` | runtime DB contract 버전                          |
| `epubContractChange`     | EPUB 저장 및 조회 계약 변경 여부                  |

`requiredMigrations`가 비어 있지 않으면 DB migration을 먼저 적용하고,
Production CD의 `migration_evidence`에 승인 기록 또는 적용 증거 ID를 입력한다.

CD는 migration을 자동 실행하지 않는다.

## 6. Production 배포

배포 전 최신 `main` full SHA를 확인한다.

```bash
git fetch origin
git rev-parse origin/main
```

GitHub에서 다음 순서로 실행한다.

```text
Actions
  -> Backend Production CD
  -> Run workflow
  -> branch: main
```

일반 배포 입력:

| 입력                             | 값                                |
| -------------------------------- | --------------------------------- |
| `confirm_production`             | `true`                            |
| `confirm_release_sha`            | 현재 main의 full 40-character SHA |
| `resume_metadata_only`           | `false`                           |
| `migration_evidence`             | migration이 없으면 공백           |
| `bootstrap_current_git_sha`      | 공백                              |
| `bootstrap_current_image_digest` | 공백                              |

주의:

- 과거 main SHA가 아니라 실행 시점의 최신 main SHA를 입력한다.
- SHA 확인 후 main이 변경됐으면 SHA를 다시 확인한다.
- 입력 SHA의 Backend CI가 성공하지 않았으면 CD가 중단된다.
- `bootstrap_*` 입력은 최초 release metadata 구축용이다. 일반 배포에 사용하지 않는다.

## 7. Backend CD 내부 동작

Production CD는 다음 순서로 실행된다.

1. `main`, production 확인, full SHA와 필수 설정을 검증한다.
2. 해당 SHA의 Backend CI 세 개가 성공했는지 확인한다.
3. GitHub OIDC로 backend 전용 AWS role을 assume한다.
4. SSM Parameter metadata, GitHub Deployment, running container digest를 비교한다.
5. 현재 production SHA와 release SHA의 schema contract를 비교한다.
6. ECR image를 full SHA tag로 build하고 digest를 확정한다.
7. SSM Run Command로 EC2 배포 script를 실행한다.
8. running image URI, tag, digest, container health와 API를 검증한다.
9. immutable production Git tag를 생성한다.
10. SSM Parameter Store에 authoritative release metadata를 기록한다.

다음 항목이 불일치하면 자동 진행하지 않는다.

- SSM release metadata
- GitHub Deployment 기록
- `.env.prod`의 configured image/tag
- 실제 running container image digest

## 8. Metadata-only 재개

Container 배포와 smoke는 성공했지만 production tag 또는 metadata 기록만
실패한 경우에만 사용한다.

입력:

| 입력                   | 값                              |
| ---------------------- | ------------------------------- |
| `confirm_production`   | `true`                          |
| `confirm_release_sha`  | 현재 실행 중인 release full SHA |
| `resume_metadata_only` | `true`                          |

CD는 running image ref와 digest를 다시 검증한 뒤 tag와 metadata만 멱등하게
마무리한다.

Container 배포 자체가 실패했거나 running SHA가 다르면 사용하지 않는다.

## 9. Rollback 정책

Backend 자동 rollback은 구현하지 않았다.

이유:

- 단일 EC2라 staging 검증 환경이 없다.
- `docker compose up -d`는 짧은 downtime이 발생할 수 있다.
- 구버전 image가 신규 S3 EPUB 데이터를 읽지 못할 수 있다.
- 현재 DB schema가 구버전 image와 호환되지 않을 수 있다.

Rollback 전 확인:

1. DB schema 변경 여부
2. EPUB storage contract 변경 여부
3. 이전 image와 현재 DB의 호환성
4. feature flag로 문제 기능을 먼저 차단할 수 있는지
5. rollback 대상 Git tag, ECR image tag, digest

Backend CD에는 rollback input이 없다. 이전 image rollback은 별도 승인 후
수동 SSM 절차로 처리한다.

RDS snapshot restore는 일반 rollback이 아니라 disaster recovery다.

## 10. 보안 및 운영 유의사항

- GitHub Secrets에 AWS Access Key를 저장하지 않는다.
- GitHub OIDC 임시 credential만 사용한다.
- GitHub Actions에 SSH private key를 등록하지 않는다.
- SSM command argument와 log에 secret을 출력하지 않는다.
- `.env.prod`는 EC2에만 유지한다.
- Docker image에 `.env`를 포함하지 않는다.
- `latest` image tag를 사용하지 않는다.
- production tag를 다른 commit으로 이동하거나 재사용하지 않는다.
- main push가 자동 production 배포를 발생시키지 않는다.
- production 배포 concurrency는 직렬이며 진행 중인 배포를 취소하지 않는다.
- zero-downtime과 backend 자동 rollback은 현재 범위가 아니다.

## 11. Backend와 Frontend가 같이 변경될 때

기본 배포 순서:

```text
DB migration 필요 여부 확인
  -> migration이 필요하면 선행 적용
  -> Backend 배포
  -> Backend health와 API 확인
  -> Frontend 배포
  -> end-to-end 기능 확인
```

Frontend가 아직 존재하지 않는 endpoint를 호출하지 않도록 backend-first를
원칙으로 한다.

## 12. 배포 체크리스트

### 배포 전

- [ ] PR review 완료
- [ ] Backend CI 세 개 성공
- [ ] 현재 main full SHA 확인
- [ ] DB migration 필요 여부 확인
- [ ] migration evidence 확보
- [ ] rollback 영향 검토

### 배포 후

- [ ] Production CD 성공
- [ ] container 상태 `running`
- [ ] container health `healthy`
- [ ] `/api/v1/categories` smoke 성공
- [ ] running tag가 배포 SHA와 일치
- [ ] running digest가 ECR digest와 일치
- [ ] production Git tag 생성 확인
- [ ] SSM release metadata 갱신 확인
