# ABC 발표자료용 기술 스택·외부 연동 아이콘

발표 슬라이드에 바로 넣을 수 있도록 개별 파일로 정리한 아이콘 묶음입니다. SVG는 PowerPoint에 삽입해도 확대 시 선명합니다.

## 기술 스택

| 구분 | 파일 | 표시 이름 |
|---|---|---|
| Frontend | `01_react.svg` ~ `06_css-modules.svg` | React, TypeScript, Vite, React Router, Axios, CSS Modules |
| Backend | `07_openjdk.svg` ~ `11_gradle.svg` | Java/OpenJDK, Spring Boot, JWT, Hibernate/JPA, Gradle |
| DB·검증·배포 | `12_mysql.svg` ~ `19_aws.svg` | MySQL, Swagger/OpenAPI, JUnit 5, Mockito, Postman, GitHub Actions, Docker, AWS |

`15_mockito.png`만 PNG이고 나머지는 SVG입니다. 실제 프로젝트 의존성에는 `framer-motion`도 있어 `23_framer-motion.svg`를 참고용으로 포함했습니다.

## 외부 연동

| 파일 | 슬라이드 표시 이름 | 역할 |
|---|---|---|
| `24_정보나루.png` | 정보나루 | 도서 후보·ISBN, 소장 도서관 데이터 |
| `20_naver.svg` | 네이버 | 도서 메타데이터 보강 |
| `25_알라딘.jpg` | 알라딘 | 도서 정보·외부 카테고리 경로 |
| `26_네이버지도.svg` | 네이버 지도 | 도서관 위치·마커 표시 |

네이버와 네이버 지도는 같은 네이버 브랜드 아이콘을 사용합니다. PPT에서는 아이콘 아래에 각각 `네이버`와 `네이버 지도`라는 텍스트를 붙여 역할을 구분하면 됩니다.

## 출처·사용 유의

- 대부분의 SVG: [Simple Icons](https://github.com/simple-icons/simple-icons) (CC0 아이콘 데이터). 브랜드별 상표 사용 조건은 각 제공처 정책을 따릅니다.
- AWS: [Font Awesome Brands](https://github.com/FortAwesome/Font-Awesome) 아이콘.
- Mockito: [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Mockito_Logo.png) 공개 파일.
- 정보나루·알라딘: 각 서비스 웹사이트에서 받은 로고 이미지.

발표 목적의 사용을 전제로 정리한 자료입니다. 외부 배포물이나 상업적 홍보물로 재사용할 때에는 각 브랜드의 최신 상표·로고 가이드를 확인하세요.

## 제외한 항목

Swiper는 현재 프론트엔드 의존성 목록에서 확인되지 않아 이 묶음에 넣지 않았습니다.
