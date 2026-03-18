# Rare-X Backend

이 프로젝트는 Java 21 기반의 Spring Boot 애플리케이션입니다.

## 👥 팀 4TENTIAL 멤버 및 역할 (Team & Roles)

이 프로젝트는 4인 팀으로 프론트엔드와 백엔드를 분리하여 개발 및 배포되었습니다.
- 🔗 **[RARE X 프론트엔드 리포지토리 가기](https://github.com/Limdonghan/rare-x-front)**
- 🔗 **[RARE X 백엔드 리포지토리 가기](https://github.com/Limdonghan/rare-x-back)**

| 이름 | 포지션 | 담당 업무 및 기여 | GitHub |
|---|---|---|---|
| **임동한 (Leader)** | Infra / Back-end / Front-end | - 팀 리딩 및 프로젝트 아키텍처 설계<br>- TossPayments API 연동<br>- Passwordless API 연동<br>- 비동기 처리를 위한 Kafka<br>- Vercel CI/CD 파이프라인 구축 | [@Limdonghan](https://github.com/Limdonghan) |
| **이조은** | Infra / Back-end | - Spring Boot 기반 REST API 개발<br>- AWS S3 파일 업로드<br>- Daum Juso API 연동<br>- GitHub Actions & AWS 활용 CI/CD 파이프라인 구축 | [@leejoeun](https://github.com/leejoeun) |
| **구본율** | Back-end | - Spring Boot 기반 REST API 개발<br>- Spring Security & JWT & Redis 기반 인증/인가 처리<br>- Typesense 검색 엔진 연동 및 최적화<br>- SMTP 기반 이메일 알림 발송 | [@gubonyul123-code](https://github.com/gubonyul123-code) |
| **임종덕** | Front-end | - React, TypeScript 기반 프론트엔드 UI/UX 구현<br>- Axios API 통신 | [@deok2525](https://github.com/deok2525) |

## 기술 스택 (Tech Stack)

### Core
- **Java**: JDK 21 (최신 기능 및 향상된 메모리/가비지 컬렉터 지원)
- **Spring Boot**: 3.5.9
- **Build Tool**: Gradle (최신 빌드 자동화 및 의존성 관리)

### Data & DB
- **Spring Data JPA**: 객체 지향 (ORM) 기반의 데이터 엑세스
- **MySQL**: RDBMS, 안정적인 데이터 저장소로 MySQL 활용
- **Redis**: 세션 클러스터링 및 데이터 캐싱을 위한 인메모리 Redis 활용
- **Typesense**: 고성능 텍스트 검색 기능을 지원하기 위한 검색 엔진 연동

### Security & Auth
- **Spring Security**: 애플리케이션의 엔드포인트 접근 권한 및 내부 롤 모델링 관리 보안
- **JWT (jjwt)**: Stateless한 인증 기반의 JSON Web Token 발급/검증
- **Spring Session Core**: 확장 가능한 분산형 세션 스토어 관리

### Messaging & Streaming
- **Spring Kafka**: Apache Kafka를 통한 비동기 이벤트 프로듀서/컨슈머 스트리밍 (예: 입찰, 알림 처리)
- **Spring WebFlux**: 리플렉티브 API 지원 및 실시간 SSE(Server-Sent Events) 알림 전송을 위한 모듈
- **Spring Boot Mail**: SMTP 정보를 기반으로 이메일 알람 발송 지원

### Cloud & Third-party
- **AWS S3**: 클라우드 객체 스토리지 연동 (사진 등 파일 업로드 전용 처리 등 지원)

### Quality & Docs
- **Lombok**: 반복되는 Getter/Setter, Constructor 보일러플레이트 코드 제거
- **Springdoc OpenAPI**: Swagger UI를 활용하여 자동으로 REST API 명세서를 생성 및 로컬 문서화
- **Spring Boot Actuator**: 애플리케이션의 상태(Health check) 및 모니터링 측정
- **SonarQube**: 플러그인을 통한 정적 코드 품질 통합 분석 (기술 부채 관리 용도)

## 구동 방법 및 환경 설정 요건

1. DB, Redis, Kafka 브로커 설정, AWS S3 Credentials(`application.properties` 와 환경변수로 분리된 부분)를 로컬 혹은 클라우드 환경에 맞게 세팅해 주세요.
2. 루트 디렉토리에서 `./gradlew build` 명령을 통해 전체 테스트 실행 및 애플리케이션을 빌드할 수 있습니다.
3. 컴파일이 정상 완료되면 `./gradlew bootRun` 명령으로 IDE의 도움 없이 Spring Boot 애플리케이션을 로컬 실행 가능합니다.

## 패키지 구조 (Package Structure)

```text
com.project.rare_x_back
├── common/         # 공통 응답 처리 클래스(Response Entity) 및 전역 설정
├── config/         # Security, AWS(S3), Kafka, Swagger, Web 등의 어노테이션 기반 Bean 설정 파일들
├── controller/     # 외부 요청(Request) 접수 및 응답을 위한 REST API 엔드포인트 인터페이스
├── dto/            # 클라이언트-서버 간 데이터 전송용 객체 (Request / Response)
├── entity/         # 데이터베이스 테이블 스키마에 매핑하는 JPA 엔티티 클래스 선언부
├── enums/          # 애플리케이션 비즈니스에서 사용되는 매직 스트링/상태값 관리용 Enum
├── exceptions/     # 커스텀 예외 및 애플리케이션 전역 예외 처리 핸들러 (Controller Advice)
├── repository/     # Spring Data JPA 인터페이스 (Query 메서드 작성 및 DB I/O 처리)
├── scheduler/      # 특정 시간대나 주기에 따라 동작해야하는 배치 스케줄링 잡(Job) 로직 모음
├── security/       # JWT 토큰 필터 작업 및 인증/인가(Authorization) 처리 설정들
└── service/        # 결제, 주문, 입찰, 회원 등 비즈니스의 핵심 구현 트랜잭션 로직
```
