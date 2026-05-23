FROM gradle:8.8-jdk17-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S smartflow && adduser -S smartflow -G smartflow
COPY --from=builder /app/build/libs/*.jar app.jar
USER smartflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
