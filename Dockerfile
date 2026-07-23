# syntax=docker/dockerfile:1

# ---------- build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Wrapper와 build script를 먼저 복사해 dependency layer 캐시를 살린다.
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test \
 && cp build/libs/*-SNAPSHOT.jar /workspace/app.jar

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre

# compose healthcheck(/api/v1/categories smoke check)용 curl
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# non-root 실행 계정
RUN groupadd --system --gid 10001 abc \
 && useradd --system --uid 10001 --gid abc --home-dir /app --shell /usr/sbin/nologin abc

WORKDIR /app
COPY --from=build --chown=abc:abc /workspace/app.jar /app/app.jar

USER abc
EXPOSE 8080

ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8"

# 이 image에는 AWS key, DB password, JWT secret을 굽지 않는다.
# 모든 secret은 compose env_file(.env.prod) 또는 실행 시 환경변수로만 주입한다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
