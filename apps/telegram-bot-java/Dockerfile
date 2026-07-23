FROM maven:3.9.9-eclipse-temurin-21 AS maven-build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
COPY --from=maven-build /workspace/target/shlyapnika-telegram-bot-*.jar /app/app.jar
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --create-home taverna \
  && chown -R taverna:taverna /app
USER taverna
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl -fsS "http://127.0.0.1:${PORT:-8081}/actuator/health/readiness" || exit 1
CMD ["java", "-jar", "/app/app.jar"]
