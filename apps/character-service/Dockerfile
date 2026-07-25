FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/character-service-0.1.0.jar /app/character-service.jar
EXPOSE 4192
ENTRYPOINT ["java", "-jar", "/app/character-service.jar"]
