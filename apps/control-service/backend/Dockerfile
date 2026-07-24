FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/control-service-backend-0.1.0.jar /app/control-service-backend.jar
EXPOSE 4190
ENTRYPOINT ["java", "-jar", "/app/control-service-backend.jar"]
