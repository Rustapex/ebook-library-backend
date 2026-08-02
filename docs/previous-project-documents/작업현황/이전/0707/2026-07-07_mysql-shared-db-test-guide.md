# MySQL 공유 DB 테스트 방법

## 목적

현재 팀원들이 각자 로컬 MySQL DB에서 테스트하던 상태를 하나로 합치기 위해, 이번 테스트에서는 한 대의 Windows 11 Home 노트북에서 MySQL을 계속 실행해 두고 같은 네트워크의 다른 팀원 PC들이 해당 MySQL DB에 접속하는 방식으로 공유 DB를 테스트할 예정입니다.

이 방식은 임시 개발/테스트용 공유 DB 구성입니다. 같은 와이파이 또는 같은 LAN 내부에서만 사용하고, 공유기 포트포워딩으로 외부 인터넷에 노출하지 않는 것을 권장합니다.

## 1. MySQL 버전 확인

MySQL Workbench에서 아래 쿼리를 실행합니다.

```sql
SELECT VERSION();
```

조금 더 자세히 확인하려면 아래 쿼리를 실행합니다.

```sql
SHOW VARIABLES LIKE 'version%';
```

PowerShell 또는 Git Bash에서는 아래 명령어로 확인할 수 있습니다.

```powershell
mysql --version
```

접속까지 포함해서 확인하려면 아래처럼 실행합니다.

```powershell
mysql -u root -p -e "SELECT VERSION();"
```

## 2. 공유 DB 구성 흐름

이번 공유 DB 테스트의 전체 흐름은 아래와 같습니다.

1. DB를 제공할 노트북에서 MySQL 서비스를 계속 실행합니다.
2. MySQL이 외부 PC의 접속을 받을 수 있도록 설정합니다.
3. 팀원 접속용 MySQL 계정을 따로 생성합니다.
4. Windows 방화벽에서 MySQL 포트인 `3306`을 허용합니다.
5. 팀원 PC에서 DB 제공 노트북의 내부 IP로 접속합니다.

## 3. MySQL 서비스 항상 켜두기

Windows에서 서비스 관리 화면을 엽니다.

```text
Win + R -> services.msc
```

MySQL 서비스 이름은 보통 아래와 같습니다.

```text
MySQL80
```

서비스 설정에서 아래처럼 맞춥니다.

```text
시작 유형: 자동
상태: 실행 중
```

PowerShell에서 확인하거나 실행할 수도 있습니다.

```powershell
Get-Service *mysql*
Start-Service MySQL80
```

노트북이 절전 상태로 들어가면 다른 PC에서 DB 접속이 끊길 수 있으므로 전원 설정도 변경합니다.

```text
설정 -> 시스템 -> 전원 및 배터리 -> 화면 및 절전
```

전원 연결 시에는 절전 상태로 들어가지 않도록 설정하는 것을 권장합니다.

## 4. MySQL 외부 접속 허용

MySQL 설정 파일은 보통 아래 경로에 있습니다.

```text
C:\ProgramData\MySQL\MySQL Server 8.0\my.ini
```

`ProgramData`는 숨김 폴더일 수 있으므로 파일 탐색기 주소창에 직접 붙여 넣어서 이동합니다.

`my.ini`에서 아래 설정을 찾습니다.

```ini
bind-address=127.0.0.1
```

다른 PC에서도 접속할 수 있도록 아래처럼 변경합니다.

```ini
bind-address=0.0.0.0
```

보안을 조금 더 제한하고 싶다면 DB 제공 노트북의 내부 IP를 직접 지정할 수도 있습니다.

```ini
bind-address=192.168.0.15
```

설정 변경 후 MySQL 서비스를 재시작합니다.

```powershell
Restart-Service MySQL80
```

## 5. 팀원 접속용 MySQL 계정 생성

`root` 계정을 공유하지 말고, 프로젝트 테스트용 계정을 따로 만드는 것을 권장합니다.

예를 들어 DB 이름이 `abc`라면 MySQL Workbench에서 아래 SQL을 실행합니다.

```sql
CREATE USER 'abc_dev'@'%' IDENTIFIED BY '비밀번호';

GRANT ALL PRIVILEGES ON abc.* TO 'abc_dev'@'%';

FLUSH PRIVILEGES;
```

같은 와이파이 또는 같은 LAN 대역에서만 접속을 허용하고 싶다면 아래처럼 IP 대역을 제한할 수 있습니다.

```sql
CREATE USER 'abc_dev'@'192.168.0.%' IDENTIFIED BY '비밀번호';

GRANT ALL PRIVILEGES ON abc.* TO 'abc_dev'@'192.168.0.%';

FLUSH PRIVILEGES;
```

`192.168.0.%` 부분은 실제 공유기/와이파이 IP 대역에 맞게 수정해야 합니다.

## 6. DB 제공 노트북 IP 확인

DB를 제공하는 노트북에서 PowerShell을 열고 아래 명령어를 실행합니다.

```powershell
ipconfig
```

출력 결과에서 `IPv4 주소`를 확인합니다.

예시:

```text
IPv4 주소 . . . . . . . . . : 192.168.0.15
```

팀원들은 이 IP를 Hostname으로 사용해서 접속합니다.

## 7. Windows 방화벽에서 3306 포트 허용

관리자 권한 PowerShell에서 아래 명령어를 실행합니다.

```powershell
New-NetFirewallRule `
  -DisplayName "MySQL 3306 LAN" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 3306 `
  -Action Allow
```

다른 팀원 PC에서 포트 접속이 가능한지 확인하려면 아래 명령어를 실행합니다.

```powershell
Test-NetConnection 192.168.0.15 -Port 3306
```

아래처럼 나오면 포트 연결은 성공입니다.

```text
TcpTestSucceeded : True
```

## 8. 팀원 PC에서 MySQL Workbench 접속 설정

팀원 PC의 MySQL Workbench에서 새 Connection을 만들고 아래처럼 입력합니다.

```text
Hostname: 192.168.0.15
Port: 3306
Username: abc_dev
Password: 위에서 만든 비밀번호
Default Schema: abc
```

여기서 `192.168.0.15`는 DB를 제공하는 노트북의 실제 IPv4 주소로 바꿔야 합니다.

## 9. DB 통합 전 권장 순서

각자 로컬 DB를 사용하다가 하나의 DB로 합치기 전에는 아래 순서로 진행하는 것을 권장합니다.

1. 팀원들의 MySQL 버전을 먼저 확인합니다.
2. 최종 기준이 될 `schema.sql`과 `data.sql`을 정합니다.
3. DB 제공 노트북에 새 DB를 생성합니다.
4. `schema.sql`을 적용합니다.
5. `data.sql`을 적용합니다.
6. 팀원들에게 DB 제공 노트북의 IP, 접속 계정, 비밀번호를 공유합니다.
7. 각 팀원 PC에서 MySQL Workbench 또는 Spring Boot 설정으로 접속 테스트를 진행합니다.

## 10. 백업 명령어

기존 DB를 덮어쓰거나 통합하기 전에는 백업을 먼저 생성합니다.

```powershell
mysqldump -u root -p abc > abc_backup.sql
```

복구가 필요할 때는 아래처럼 사용할 수 있습니다.

```powershell
mysql -u root -p abc < abc_backup.sql
```

## 11. 주의사항

- `root` 계정은 팀원에게 공유하지 않습니다.
- 테스트용 계정은 `abc_dev`처럼 별도로 생성합니다.
- 공유기 포트포워딩은 설정하지 않습니다.
- 같은 와이파이 또는 같은 LAN 내부에서만 접속하도록 사용합니다.
- DB 제공 노트북이 꺼지거나 절전 상태가 되면 팀원 PC에서 DB 접속이 끊깁니다.
- IP가 바뀌면 팀원들의 접속 설정도 다시 바꿔야 합니다.
- 가능하면 최종 기준이 되는 `schema.sql`, `data.sql`을 먼저 정한 뒤 공유 DB에 적용합니다.
