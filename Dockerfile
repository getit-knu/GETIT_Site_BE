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
RUN chmod +x gradlew

# 의존성 jar 를 먼저 받아 레이어로 굳힌다. build.gradle 이 그대로면 이 레이어가 재사용되어
# 다음 빌드는 Maven Central 을 아예 건드리지 않는다.
#
# 기본 `dependencies` 태스크는 그래프를 출력할 뿐이라 pom 만 받고 jar 은 받지 않는다.
# 그래서 이 레이어가 있어도 실제 다운로드는 매번 아래 bootJar 단계에서 처음부터 일어났다.
# resolveDependencies 는 파일까지 받는다 (build.gradle 참고).
#
# 재시도를 감싼 이유: 러너에서 한 번에 수백 개를 받으면 Maven Central 이 429
# (Too Many Requests) 를 돌려준다. GHA 캐시가 비어 전부 새로 받는 날 실제로 터졌다
# (CD 33349026745). 코드 문제가 아니라 남의 서버 사정이므로 기다렸다 다시 받는다.
#
# 예전에는 이 줄이 `|| true` 로 실패를 삼켰다. 그러면 절반만 받은 상태가 성공한 레이어로
# 굳어서, 정작 bootJar 가 나머지를 받다가 같은 429 에 걸린다. 실패하면 실패하게 둔다.
RUN for attempt in 1 2 3 4 5; do \
      ./gradlew resolveDependencies --no-daemon --quiet && break; \
      echo "의존성 내려받기 실패 ($attempt/5). 30초 뒤 다시 시도한다."; \
      [ "$attempt" = 5 ] && exit 1; \
      sleep 30; \
    done

COPY src src
RUN for attempt in 1 2 3; do \
      ./gradlew bootJar --no-daemon -x test && break; \
      echo "빌드 실패 ($attempt/3). 30초 뒤 다시 시도한다."; \
      [ "$attempt" = 3 ] && exit 1; \
      sleep 30; \
    done

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
