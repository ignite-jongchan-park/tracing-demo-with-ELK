# 멀티스테이지: build 단계는 모든 모듈을 한 번에 빌드하며 4개 서비스가 공유(BuildKit 캐시).
# MODULE 인자는 runtime 단계에서만 쓰이므로 build 단계 캐시는 서비스마다 재사용된다.
FROM gradle:9.1.0-jdk25 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle settings.gradle build.gradle gradle.properties ./
COPY --chown=gradle:gradle common ./common
COPY --chown=gradle:gradle gateway ./gateway
COPY --chown=gradle:gradle order-service ./order-service
COPY --chown=gradle:gradle inventory-service ./inventory-service
COPY --chown=gradle:gradle payment-service ./payment-service
RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:25-jdk AS runtime
ARG MODULE
WORKDIR /app
COPY --from=build /home/gradle/src/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
