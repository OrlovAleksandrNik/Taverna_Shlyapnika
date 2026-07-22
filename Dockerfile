FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/apps/backend-java
COPY apps/backend-java/pom.xml ./pom.xml
COPY apps/backend-java/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Railway root deployment: one Java service serves both API and the static site.
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV SERVE_FRONTEND=true
ENV FRONTEND_STATIC_DIR=/app/static-site
ENV FILE_STORAGE_DIR=/app/uploads
ENV PUBLIC_UPLOADS_URL=/uploads
COPY --from=backend-build /workspace/apps/backend-java/target/shlyapnika-backend-*.jar /app/app.jar
COPY assets /app/static-site/assets
COPY data /app/static-site/data
COPY masters /app/static-site/masters
COPY *.html /app/static-site/
COPY robots.txt sitemap.xml /app/static-site/
COPY styles.css script.js /app/static-site/
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --create-home taverna \
  && mkdir -p /app/uploads \
  && chown -R taverna:taverna /app
USER taverna
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health/readiness" || exit 1
CMD ["java", "-jar", "/app/app.jar"]
