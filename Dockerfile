# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy AS builder


WORKDIR /app


COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle


COPY build.gradle .
COPY settings.gradle .


RUN ./gradlew dependencies --no-daemon


COPY src ./src


RUN ./gradlew build -x test --no-daemon


FROM openjdk:17-jdk-slim

WORKDIR /app



ARG JAR_FILE=build/libs/cooked_backend-0.0.1-SNAPSHOT.jar
COPY --from=builder /app/${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]