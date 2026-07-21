# AcornProject

## 백엔드 로컬 실행

백엔드는 로컬 MySQL 8.x의 `ABC` 데이터베이스를 사용합니다.

> DB 생성, migration 또는 기존 DB 보정 작업 전에는 반드시
> [`docs/데이터베이스/README.md`](docs/데이터베이스/README.md)를 먼저 확인하세요.
> `docs/데이터베이스/ddl/schema.sql`은 실행용 최종 DDL이 아닙니다.
> AI agent도 이 문서를 완전히 읽고 DB 상태를 판정하기 전에는 SQL을 실행하면 안 됩니다.

1. 데이터베이스가 없으면 먼저 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS ABC DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. 신규 DB라면 [`docs/데이터베이스/README.md`](docs/데이터베이스/README.md)의 "신규 빈 DB" 절차에 따라 `schema.sql`과 필수 sidecar 2개를 먼저 적용합니다. 기존 DB라면 같은 문서의 read-only 상태 판별부터 수행합니다.

3. `.env.example` 파일을 `.env`로 복사하고 로컬 MySQL 접속 정보를 입력합니다.

```properties
DB_URL=jdbc:mysql://localhost:3306/ABC?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

4. 백엔드를 실행합니다.

```powershell
.\gradlew.bat bootRun --console=plain
```

`Access denied ... (using password: NO)` 오류가 나오면 Spring에 `DB_PASSWORD`가 전달되지 않은 상태입니다. 프로젝트 루트에 `.env`가 있는지 확인하거나, 실행 전에 PowerShell에서 직접 환경변수를 설정하세요.

```powershell
$env:DB_PASSWORD = "your_mysql_password"
.\gradlew.bat bootRun --console=plain
```
