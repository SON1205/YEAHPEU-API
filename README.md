# yeahpeu-API

Spring Boot 기반의 `yeahpeu` 백엔드 API 서버입니다.

## Requirements

- Java 21
- Docker

## Environment Variables

`.env.example`을 복사해서 `.env`를 만든 뒤 값을 채웁니다.

```bash
cp .env.example .env
```

기본 로컬 실행에 필요한 값:

- `SPRING_PROFILES_ACTIVE=dev`
- `DATASOURCE_URL`
- `DATASOURCE_USERNAME`
- `DATASOURCE_PASSWORD`
- `APPLICATION_SECURITY_JWT_SECRET_KEY`

OAuth, OpenAI, Naver, AWS, Mail 설정은 사용하는 기능에 맞게 채우면 됩니다.

## Run MySQL

로컬 MySQL 실행:

```bash
docker compose -f docker-compose-local.yml up -d
```

초기 스키마와 데이터는 `db/schema.sql`, `db/init.sql`에서 적재됩니다.

기존 볼륨까지 포함해 완전히 다시 올리려면:

```bash
docker compose -f docker-compose-local.yml down -v
docker compose -f docker-compose-local.yml up -d
```

## Run Application

애플리케이션 실행:

```bash
./gradlew bootRun
```

이 저장소의 `gradlew`는 macOS에서 `JAVA_HOME`이 비어 있으면 Java 21을 우선 사용하도록 설정되어 있습니다.

## Test

테스트 실행:

```bash
./gradlew test
```

## Profiles

- `dev`: 로컬 개발용 기본 프로필
- `prod`: 운영용 프로필

운영 프로필 실행 예시:

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```
