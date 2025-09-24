# Etapa 1: Build con Gradle
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Etapa 2: Imagen final
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","sistema-0.0.1-SNAPSHOT.jar"]
