# AcornProject

## 백엔드 로컬 실행

백엔드는 로컬 MySQL 8.x의 `ABC` 데이터베이스를 사용합니다.

1. 데이터베이스가 없으면 먼저 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS ABC DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. `.env.example` 파일을 `.env`로 복사하고 로컬 MySQL 접속 정보를 입력합니다.

```properties
DB_URL=jdbc:mysql://localhost:3306/ABC?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

3. 백엔드를 실행합니다.

```powershell
.\gradlew.bat bootRun --console=plain
```

`Access denied ... (using password: NO)` 오류가 나오면 Spring에 `DB_PASSWORD`가 전달되지 않은 상태입니다. 프로젝트 루트에 `.env`가 있는지 확인하거나, 실행 전에 PowerShell에서 직접 환경변수를 설정하세요.

```powershell
$env:DB_PASSWORD = "your_mysql_password"
.\gradlew.bat bootRun --console=plain
```
