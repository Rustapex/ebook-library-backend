# Java·Spring Boot 백엔드 포트폴리오 참고 자료

> 조사일: 2026-07-26  
> 목적: ABC 전자책 대여 서비스 개인 포트폴리오를 편집할 때 참고할 유사 포트폴리오와 GitHub 프로젝트 문서 정리

## 1. 이 문서의 사용 범위

이 문서는 다음 두 포트폴리오 초안의 외부 참고 자료다.

- `2026-07-22_개인문서_초안.md`
  - ABC 전자책 대여 서비스에서 담당한 사용자·관리자 API 전체 범위와 문제 해결 과정을 정리한 개인 개발 보고서
- `2026-07-22_포트폴리오_독서보상API_초안.md`
  - 독서 보상의 중복 수령 방지, 보상 변경 정책, 포인트·쿠폰 실제 지급과 트랜잭션 일관성을 대표 사례로 압축한 제출용 포트폴리오 초안

외부 사례의 문구나 성과를 그대로 가져오기 위한 자료가 아니다. 다음 항목을 비교해 ABC 포트폴리오의 전달 방식을 개선하는 데 사용한다.

1. 프로젝트의 문제와 목적이 먼저 보이는가
2. Skills만 나열하지 않고 실제 프로젝트와 개인 역할을 설명하는가
3. 프로젝트마다 문제·기능·기술·성과·링크를 제공하는가
4. 포트폴리오, README, 소스, 배포 링크가 실제로 연결되는가
5. Java, Spring Boot, MySQL, REST API, AWS, Git 협업 근거가 있는가

---

## 2. 현재 ABC 포트폴리오의 비교 기준

### 프로젝트와 개인 역할

- 프로젝트: ABC(Acorn Book Cloud) 전자책 대여 플랫폼
- 개인 역할: 부팀장, DB 설계, 백엔드 API 개발, 검증, 발표
- 담당 흐름:
  - 도서·카테고리·컬렉션 탐색
  - 전자책 열람, 진행도, 북마크, 최근 읽은 책
  - 출석, 챌린지, 포인트, 쿠폰의 독서 보상
  - 관리자 대시보드, 통계, 감사 로그, 도서 관리

### 대표 프로젝트 사례

독서 보상 API는 다음 문제를 해결한 사례다.

- 달성 시점과 수령 시점 중 언제 보상을 확정할지 정책으로 결정
- 같은 보상에 여러 요청이 들어와도 한 요청만 수령권을 선점
- 수령권 선점과 포인트·쿠폰 지급을 하나의 트랜잭션으로 처리
- 지급 실패 시 수령 완료 상태도 함께 롤백
- 포인트 잔액·이력과 회원 쿠폰의 실제 생성 결과를 DB에서 검증

### 기술과 협업 근거

- Java 17, Spring Boot 4.0.7
- Spring Data JPA, Spring Security, JWT
- MySQL, Gradle, Swagger/OpenAPI
- Backend: EC2, Docker, ECR, SSM
- Frontend: S3, CloudFront
- GitHub Actions CI/CD
- API 명세·화면·런타임 스키마 대조
- 코드리뷰 지적을 API 계약, 동시성 처리, 검증 시나리오 보완으로 확장

현재 ABC 포트폴리오의 가장 큰 강점은 기술 개수가 아니라 **운영 중 변경되는 상태와 중복 요청을 일관된 최종 데이터로 만드는 과정**이다. 외부 사례도 이 강점을 더 짧고 명확하게 보여 주는 관점에서 선정했다.

---

## 3. 최종 선정한 참고 포트폴리오

| 우선순위 | 포트폴리오 | 주 참고 목적 | ABC에 적용할 요소 | 주의할 점 |
|---:|---|---|---|---|
| 1 | [Aggelos Ladas](https://aggelosladas.com/) | Spring Boot 백엔드 프로젝트 표현 | 역할, REST API, 인증, DB, 실행 방법을 한 흐름으로 제시 | 정량 성과와 실제 배포 링크는 부족 |
| 2 | [Paul Atienza](https://www.paulatienza.dev/) | 프로젝트 문제와 존재 이유 설명 | 실제 업무·개인 문제에서 프로젝트가 출발한 이유, 검증 가능한 범위 명시 | 메인 포트폴리오는 Spring Boot보다 Enterprise Java 경력 중심 |
| 3 | [Rahul Kushwaha](https://raahullkushwaha.dev/) | 백엔드 아키텍처와 프로젝트 분류 | 기능, 데이터 저장소, 메시징, 실시간 처리, 테스트 구조 설명 | 일부 README와 배포 링크 품질이 일정하지 않음 |
| 4 | [Aditya Srivastava](https://adityasri.in/) | 수치 중심 결과 표현 | 사용자 수, 처리량, 성능, 배포 시간, 테스트 결과를 성과 문장으로 연결 | 링크 오류와 README 부재가 있어 주장 검증 필요 |
| 5 | [Manjunath Bhandari](https://manjunathbhandari.vercel.app/) | 풀스택 프로젝트 카드 구성 | 설명, Key Features, Tech Stack, GitHub, Live Demo의 반복 구조 | 일부 저장소 README가 기본 템플릿이거나 비어 있음 |

---

## 4. 참고 사례 1 — Aggelos Ladas

### 링크

- 포트폴리오: [aggelosladas.com](https://aggelosladas.com/)
- 대표 프로젝트: [ICE E-Shop](https://github.com/AggelosLadas/ICE-E-Shop)

### 확인한 특징

포트폴리오를 API 문서와 비슷한 형태로 구성하고, 프로젝트마다 본인이 구현한 영역을 짧게 설명한다. ICE E-Shop README에는 다음 정보가 있다.

- Spring Boot 기반 전체 백엔드 개발
- RESTful API와 JWT 인증
- Hibernate/JPA와 MySQL
- Swagger UI
- Docker 실행 방법
- 화면 스크린샷
- 백엔드와 프런트엔드 담당자 구분

기술을 단순히 나열하지 않고 인증, 데이터 접근, API 문서, 실행 환경에서 각각 어떤 역할을 하는지 연결한 점이 좋다.

### ABC 포트폴리오에 적용할 부분

독서 보상 사례의 첫 화면 또는 첫 페이지에 다음 정보를 한 카드로 압축할 수 있다.

```text
프로젝트: ABC 전자책 대여 플랫폼
담당 영역: 독서 보상 백엔드 API 및 관리자 보상 관리
핵심 문제: 중복 요청과 운영 중 보상 변경에도 일관된 지급 결과 보장
핵심 기술: Spring Boot, JPA, MySQL, REST API, 트랜잭션, 조건부 갱신
검증: 자동 테스트, Swagger/Postman, DB 전후 상태 확인
링크: GitHub / API 문서 / 배포 화면 / 상세 포트폴리오
```

README 또는 상세 본문에서는 다음 순서를 참고한다.

1. 프로젝트 목적
2. 개인 담당 영역
3. 핵심 기능
4. 기술 선택과 사용 이유
5. 실행·검증 방법
6. 결과 화면과 관련 링크

### 그대로 따라 하지 않을 부분

- 스크린샷과 기술 스택만으로 끝내지 않는다.
- ICE E-Shop에는 부족한 정량 결과와 검증 근거를 ABC에서는 보완한다.
- 협업 프로젝트라면 “백엔드 담당”에서 끝내지 않고 본인이 설계한 정책과 API 범위를 명시한다.

---

## 5. 참고 사례 2 — Paul Atienza

### 링크

- 포트폴리오: [paulatienza.dev](https://www.paulatienza.dev/)
- Spring Boot 프로젝트: [Asi Language API](https://github.com/phatienza/asi-language-api)
- 학습 도구 프로젝트: [Learn Grep Regex](https://github.com/phatienza/learn-regex)
- 배포 사이트: [Learn Grep Regex](https://learn-grep-regex.paulatienza.dev/)

### 확인한 특징

프로젝트가 존재해야 하는 이유를 기술보다 먼저 설명한다.

- Learn Grep Regex:
  - 유지보수와 로그 조사 중 정규식이 필요했던 실제 경험에서 출발
  - 지원 기능, 17개 학습 단계, 테스트·빌드 방법을 명시
  - 현재 지원 범위와 의도적으로 제외한 범위를 구분
- Asi Language API:
  - 지역 언어 보존이라는 명확한 목적
  - API 엔드포인트 표와 요청·응답 예시
  - 데이터 출처
  - 테스트 방법
  - Java 21, Spring Boot, JPA, PostgreSQL, Swagger, Railway 배포 정보

기능의 수보다 **누구의 어떤 문제를 해결하는 프로젝트인지**가 먼저 기억되게 구성한다.

### ABC 포트폴리오에 적용할 부분

독서 보상 API도 다음과 같이 기술보다 문제를 먼저 제시한다.

> 챌린지를 달성했다는 화면 상태만으로는 실제 보상 지급을 보장할 수 없다. 중복 요청, 보상 설정 변경, 지급 중 실패가 발생해도 사용자의 포인트·쿠폰과 수령 상태가 서로 다른 결과를 가리키지 않게 만들어야 했다.

상세 본문에는 다음 정보를 보강한다.

- 관련 REST API 목록
- 정상·중복·실패 요청 예시
- 대표 응답 JSON
- 포인트 이력과 회원 쿠폰의 DB 전후 상태
- 자동 테스트와 수동 검증 명령
- 현재 구현 범위와 향후 개선 범위

### 그대로 따라 하지 않을 부분

- 지역 언어 보존처럼 감성적인 동기를 억지로 추가하지 않는다.
- ABC에서는 실제 프로젝트의 운영 정책, 동시성, 데이터 정합성을 중심으로 쓴다.
- 구현하지 않은 배포·모니터링·성능 결과를 향후 계획과 현재 성과에 섞지 않는다.

---

## 6. 참고 사례 3 — Rahul Kushwaha

### 링크

- 포트폴리오: [raahullkushwaha.dev](https://raahullkushwaha.dev/)
- 대표 백엔드 프로젝트: [Instagram Backend Clone](https://github.com/raahulllkushwaha/Instagram-Backend-Clone)
- 실시간 프로젝트: [Real-Time Chat App](https://github.com/raahulllkushwaha/Real-Time-Chat-App)
- 문서가 부족한 비교 사례: [BookMyShow](https://github.com/raahulllkushwaha/BookMyShow)

### 확인한 특징

프로젝트 카드에 목적, 핵심 기능, 기술, 상태, 소스 링크를 반복해서 제공한다. Instagram Backend Clone은 README와 실제 소스에서 다음 구조를 확인할 수 있었다.

- PostgreSQL과 Neo4j를 함께 사용하는 데이터 구조
- Kafka 기반 알림
- WebSocket/STOMP 기반 실시간 채팅
- JWT 인증과 요청 제한
- Docker Compose
- 기능별 패키지 구조
- JUnit·Mockito 테스트

README에만 이름이 있는 것이 아니라 Kafka producer, Neo4j repository, WebSocket 설정, 인증·좋아요 서비스 테스트 파일이 실제 저장소에 존재한다.

### ABC 포트폴리오에 적용할 부분

ABC 독서 보상 사례에서는 프레임워크 이름보다 다음 처리 경계를 시각적으로 보여 주는 데 참고한다.

```text
보상 수령 요청
  → 달성·미수령 상태 확인
  → 조건부 갱신으로 수령권 선점
  → 포인트 잔액·이력 또는 회원 쿠폰 생성
  → 성공 시 전체 확정
  → 실패 시 수령 상태까지 전체 롤백
```

프로젝트 상세 설명에는 다음 책임을 분리한다.

- Controller: 요청 검증과 응답 계약
- Service: 보상 정책, 수령권 선점, 트랜잭션
- Repository: 조건부 갱신과 데이터 조회
- Database: 중복·상태·이력의 최종 정합성
- Test: 최신 보상 지급, 중복 차단, 지급 실패 롤백 검증

### 그대로 따라 하지 않을 부분

- 프로젝트 개수를 늘리기 위해 완성도가 낮은 저장소를 함께 노출하지 않는다.
- Rahul의 BookMyShow README는 제목 한 줄뿐이므로 문서 참고 대상이 아니다.
- Real-Time Chat 배포 사이트는 조사 시점에 Render 로딩 화면에서 벗어나지 못했다.
- README 내부의 실제 배포 URL과 “추후 링크 추가” 같은 모순된 문구를 남기지 않는다.

---

## 7. 참고 사례 4 — Aditya Srivastava

### 링크

- 포트폴리오: [adityasri.in](https://adityasri.in/)
- 확인 가능한 프로젝트 소스: [Spring Boot AI Chat App](https://github.com/adityaSrivastava29/springbootAIChatApp)

### 확인한 특징

Java, Spring Boot, React, REST API, Redis, AWS, Docker, Jenkins, GitHub Actions, JUnit·Mockito를 경력과 결과에 연결한다. 다음과 같은 성과 문장 구조를 사용한다.

```text
무엇을 구현했는가
  + 어떤 규모에서 사용됐는가
  + 어떤 기술적 조치를 했는가
  + 시간이거나 비율로 무엇이 개선됐는가
```

예를 들면 사용자 수, 월간 이벤트 처리량, API 응답 시간, DB 부하, 배포 시간, 테스트 커버리지 등을 함께 제시한다.

### ABC 포트폴리오에 적용할 부분

ABC에서 실제 근거가 있는 결과만 다음처럼 연결한다.

- 자동 테스트 성공 건수
- 동일 보상 재수령 차단 결과
- 포인트 잔액과 이력의 동시 반영
- 쿠폰 보상 수량과 실제 `member_coupon` 생성 건수 일치
- 지급 실패 시 수령 시각까지 롤백된 테스트 결과
- GitHub Actions와 AWS 배포 흐름
- 코드리뷰 지적 사항을 보완한 API 계약과 검증 시나리오

성과 수치가 없더라도 다음처럼 전후 상태로 표현할 수 있다.

```text
Before: 수령 완료 상태와 실제 지급 데이터가 분리될 위험
After: 수령권 선점과 실제 지급을 하나의 트랜잭션으로 묶어 함께 성공하거나 함께 롤백
```

### 그대로 따라 하지 않을 부분

- 외부 포트폴리오의 사용자 수, 성능 개선율, 커버리지 수치를 가져오지 않는다.
- 본인이 재현하거나 문서로 증명할 수 없는 수치는 사용하지 않는다.
- 해당 사이트는 일부 프로젝트 코드·데모 링크가 GitHub 프로필이나 홈페이지로 잘못 연결되어 있다.
- Spring Boot AI Chat App 저장소에는 실제 소스가 있지만 README가 없으므로 링크만으로 문서화가 끝났다고 판단하지 않는다.

---

## 8. 참고 사례 5 — Manjunath Bhandari

### 링크

- 포트폴리오: [manjunathbhandari.vercel.app](https://manjunathbhandari.vercel.app/)
- 프로젝트: [28 Miles](https://github.com/manjunathbhandari98/28-miles)
- 배포 사이트: [28 Miles](https://28-miles.vercel.app/)
- 비교 대상: [DineBoard](https://github.com/manjunathbhandari98/DineBoard)

### 확인한 특징

프로젝트마다 다음 구성을 동일하게 반복한다.

1. 프로젝트 한 문장 설명
2. Key Features
3. Tech Stack
4. GitHub
5. Live Demo

28 Miles, DineBoard, Invizo 등의 배포 사이트는 조사 시점에 열렸다. 풀스택 프로젝트에서 프런트 화면과 Spring Boot 백엔드가 함께 있다는 점을 짧게 전달하기 좋다.

### ABC 포트폴리오에 적용할 부분

대표 사례 3개를 선택해 같은 카드 형식으로 정리한다.

| 대표 사례 | 핵심 문제 | 핵심 기능 | 기술 | 증거 링크 |
|---|---|---|---|---|
| 독서 보상 | 중복 수령과 부분 지급 | 수령권 선점, 포인트·쿠폰 실제 지급, 롤백 | Spring Boot, JPA, MySQL, 트랜잭션 | 상세 문서, API, DB 결과 |
| 전자책 열람 | 실제 데이터 구조와 명세 불일치 | 페이지 조회, 진행도, 북마크, 최근 읽은 책 | REST API, JPA, MySQL | 상세 문서, Swagger |
| 관리자 운영 | 상태 변경과 이력 추적 | 통계, 감사 로그, 도서 변경 | Spring Security, 통계 쿼리, 감사 로그 | 상세 문서, 관리자 화면 |

### 그대로 따라 하지 않을 부분

- DineBoard 프런트 저장소 README는 기본 Vite 템플릿 상태다.
- 일부 Spring Boot 백엔드 저장소에는 README가 없다.
- 카드에 기술과 링크가 있어도 저장소 문서가 비어 있으면 완성된 포트폴리오로 보지 않는다.
- 프런트와 백엔드 저장소가 분리됐다면 두 링크를 모두 표시한다.

---

## 9. 검토했지만 최종 5개에서 제외한 사례

| 사례 | 제외 이유 | 제한적으로 참고할 부분 |
|---|---|---|
| [Mohit Kumar](https://mohit1r.github.io/Portfolio/) | 프로젝트 소스·배포·README 링크가 없음 | 백엔드 프로젝트를 2~3문장으로 압축하는 방식 |
| [Camel Djoulako](https://cameldjoulako.com/) | 일부 내부 프로젝트 상세 경로가 404 | 비즈니스 문제, 기능, 기술을 한 카드에 연결하는 방식 |
| [Kunal Patil](https://www.kunalpatil.me/#work) | 프로젝트 상세 페이지 404, 프로젝트 섹션 근거 부족 | 경력과 기술을 시각적으로 배치하는 방식 |
| [Mukesh](https://themukesh.com/) | 현재 사이트는 React·Node 중심이며 프로젝트 링크가 없음 | 사용하지 않음 |
| [Hugo Calmels](https://hugo-calmels.fr/) | 구체적인 프로젝트 포트폴리오가 없는 서비스 소개 사이트 | 사용하지 않음 |

---

## 10. ABC 포트폴리오에 적용할 최종 구성

### 10.1 첫 페이지

```text
운영 중 변경되는 보상을 안전하게 지급한 독서 보상 API

Java / Spring Boot 백엔드 개발자

같은 보상에 여러 요청이 들어와도 한 요청만 수령권을 선점하게 하고,
선점과 포인트·쿠폰 지급을 하나의 트랜잭션으로 묶어
지급 실패 시 수령 완료 상태도 남지 않도록 구현했다.
```

함께 표시할 정보:

- 프로젝트 기간
- 개인 역할
- 대표 기술
- GitHub
- 배포 화면
- 상세 README 또는 PDF

### 10.2 프로젝트 본문

1. 프로젝트와 개인 역할
2. 문제 정의
3. 정책 대안과 선택 이유
4. 동시성 제어
5. 트랜잭션과 데이터 변경
6. REST API와 응답 예시
7. 자동 테스트와 수동 검증
8. 결과와 현재 한계
9. GitHub·Swagger·배포·증거 링크

### 10.3 프로젝트 카드 필수 항목

```text
프로젝트명:
기간:
개인 역할:
해결한 문제:
핵심 기능:
기술과 선택 이유:
검증 결과:
성과 또는 전후 변화:
GitHub:
README:
Swagger/API:
배포 또는 시연:
```

### 10.4 링크 검수 기준

- [ ] 포트폴리오의 모든 GitHub 링크가 개별 저장소로 연결된다.
- [ ] 프런트와 백엔드가 분리된 경우 두 저장소를 모두 표시한다.
- [ ] README에 목적, 기능, 기술, 실행법, 테스트, 한계가 있다.
- [ ] 배포 링크가 실제 화면으로 열리고 로딩·404 상태가 아니다.
- [ ] Swagger 또는 대표 요청·응답을 확인할 수 있다.
- [ ] 프로젝트 카드의 기술과 저장소의 실제 의존성이 일치한다.
- [ ] GitHub Actions, AWS, Docker 등은 실제 구성 또는 실행 근거와 함께 표시한다.
- [ ] 정량 성과는 테스트 결과, 로그, DB 결과 등으로 설명할 수 있다.

---

## 11. 최종 참고 원칙

ABC 포트폴리오는 프로젝트 수를 늘리는 방향보다 다음 세 가지를 명확히 하는 방향이 적합하다.

1. **문제:** 중복 요청과 운영 중 상태 변경이 왜 데이터 신뢰를 해칠 수 있었는가
2. **판단:** 어떤 정책과 트랜잭션 경계를 선택했고 다른 대안은 왜 선택하지 않았는가
3. **증거:** 자동 테스트, API 응답, DB 전후 상태, 리뷰 기록으로 결과를 어떻게 확인했는가

외부 사례에서 가져올 최종 조합은 다음과 같다.

- Aggelos: Spring Boot 백엔드 역할과 기술 구조
- Paul: 프로젝트 문제와 존재 이유
- Rahul: 아키텍처와 데이터 흐름
- Aditya: 검증 가능한 결과 표현 방식
- Manjunath: 반복 가능한 프로젝트 카드와 링크 배치

이 조합을 사용하되, ABC만의 차별점인 **보상 정책, 중복 수령 방지, 원자적 지급, 실제 DB 검증, 코드리뷰 대응**을 본문의 중심으로 유지한다.
