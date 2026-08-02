# E-Book Library Backend

전자책 대여 플랫폼을 독립적으로 고도화하는 Spring Boot REST API 저장소입니다.

기존 구현을 출발점으로 삼되, 미완성 기능 보완, 구조 개선, 리팩터링, 테스트 강화와 배포 체계 개선을 지속적으로 진행합니다.

> 출발점 참고: [AcornProject](https://github.com/shangpa/AcornProject)
>
> 이 저장소는 위 프로젝트와 별도의 개인 개발·개선 이력을 관리합니다.

## 목표

- 미완성 도메인 기능 완성
- API, 예외 처리, 검증 및 테스트 품질 개선
- 데이터 모델과 쿼리 구조 개선
- Docker 및 GitHub Actions 기반 개발·검증 체계 고도화

## 예정 기술 스택

- Java 17, Spring Boot, Spring Data JPA
- MySQL, Gradle
- Docker / Docker Compose
- GitHub Actions

## 개발 흐름

```text
feature/* → Pull Request → verify CI → squash merge → main
```

초기 소스와 참고 문서는 단계적으로 옮깁니다. 실제 환경 변수, 인증서, 개인 키와 운영 자격 증명은 저장소에 올리지 않습니다.

## 관련 저장소

- Frontend: [ebook-library-frontend](https://github.com/Rustapex/ebook-library-frontend)
