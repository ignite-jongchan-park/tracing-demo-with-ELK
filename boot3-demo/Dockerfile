# Boot 3 최소 데모: 컨테이너 안에서 Java 21 + Gradle 8 로 빌드/실행.
FROM gradle:8.14-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:21-jdk AS runtime
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
