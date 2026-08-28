# syntax=docker/dockerfile:1

# =============================================================================
# 빌드 — 의존성 캐시를 살리려고 소스보다 build.gradle 을 먼저 복사한다.
# 테스트는 여기서 돌리지 않는다. CI 가 이미 검증한 커밋만 이미지로 만든다.
# =============================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 레이어로 쪼갠다. 의존성은 잘 안 바뀌므로 재배포 시 이 레이어를 그대로 재사용한다.
RUN cp build/libs/*-SNAPSHOT.jar app.jar 2>/dev/null || cp build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# =============================================================================
# 실행 — JRE 만 담는다. JDK 를 그대로 쓰면 이미지가 두 배가 되고 공격 면이 넓어진다.
# =============================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 로그 타임스탬프를 서비스 타임존에 맞춘다. 애플리케이션 타임존은 Spring 이 따로 고정한다.
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# root 로 돌리지 않는다.
RUN groupadd --system getit && useradd --system --gid getit --home /app getit

# 업로드 경로를 이미지 안에 미리 만들어 소유권을 넘긴다.
#
# 도커는 named volume 을 마운트할 때, 그 경로가 이미지에 없으면 root 소유로 새로 만든다.
# 그러면 uid 999 로 도는 앱이 LocalFileStorage 에서 파일을 쓰지 못하고 권한 오류로 실패한다.
# 경로가 이미지에 있으면 도커가 그 소유권을 빈 볼륨에 그대로 복사한다.
RUN mkdir -p /data/uploads && chown -R getit:getit /data

COPY --from=builder --chown=getit:getit /build/extracted/dependencies/ ./
COPY --from=builder --chown=getit:getit /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=getit:getit /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=getit:getit /build/extracted/application/ ./

USER getit
EXPOSE 8080

# 컨테이너에 할당된 메모리를 JVM 이 인식하게 한다. App Service 플랜을 바꿔도 따라간다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
