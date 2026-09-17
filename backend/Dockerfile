# syntax=docker/dockerfile:1

# ── 1단계: 빌드 ───────────────────────────────────────────────
# JDK가 필요한 것은 빌드할 때뿐이다. 결과물(jar)만 다음 단계로 넘긴다.
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# 의존성 목록만 먼저 복사하면, 소스만 바뀐 경우 이 레이어를 캐시에서 재사용한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# layered jar를 펼친다. 자주 바뀌지 않는 라이브러리와 자주 바뀌는 앱 코드를
# 다른 레이어로 나누면, 코드만 고쳤을 때 전송량이 크게 줄어든다.
# 결과: extracted/{dependencies,spring-boot-loader,snapshot-dependencies,application}/
#
# 주의: application/ 안의 jar는 이름을 바꾸면 안 된다. jar 안의 Class-Path가
# 자기 이름을 기준으로 의존성(../dependencies/lib)을 찾기 때문에, 이름을 바꾸면
# NoClassDefFoundError 가 난다. 대신 실행 시점에 jar를 찾는다.
RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --destination extracted

# ── 2단계: 실행 ───────────────────────────────────────────────
# 실행에는 JRE만 있으면 된다(이미지가 훨씬 작다).
FROM eclipse-temurin:21-jre AS runtime

# 헬스체크용 curl
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# root로 실행하지 않는다. 컨테이너가 뚫려도 피해를 줄인다.
RUN useradd --system --create-home --uid 10001 appuser

# 업로드 디렉터리를 미리 만들고 소유자를 appuser로 지정한다.
# 이렇게 해두면 도커가 새 named volume을 만들 때 이 소유권을 그대로 가져간다.
# (빠뜨리면 볼륨이 root 소유가 되어 업로드가 Permission denied 로 실패한다.)
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app

USER appuser
WORKDIR /app

# 레이어를 "같은 디렉터리"에 차례로 펼친다.
# jar의 Class-Path가 자기 옆의 lib/ 를 가리키므로, 폴더 구조를 유지한 채 복사하면 의존성을 못 찾는다.
# 자주 바뀌지 않는 것부터 복사해야 코드만 고쳤을 때 앞 레이어가 캐시된다.
COPY --from=builder --chown=appuser:appuser /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/application/ ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport" \
    SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

# sh -c 를 쓰는 이유: JAVA_OPTS 안의 여러 옵션을 셸이 분리해 주고,
# 버전이 붙은 jar 이름을 실행 시점에 찾을 수 있기 때문이다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar *.jar"]
