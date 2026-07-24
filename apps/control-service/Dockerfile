FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json package.json
RUN npm install
COPY frontend .
RUN npm run build

FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /backend
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
COPY backend .
RUN chmod +x ./mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend-build /backend/target/control-service-backend-0.1.0.jar /app/control-service-backend.jar
COPY --from=frontend-build /frontend/dist /app/static
RUN mkdir -p /app/data/media /app/data/backups
ENV CONTROL_STATIC_LOCATIONS=file:/app/static/
ENV CONTROL_MEDIA_STORAGE_ROOT=/app/data/media
ENV CONTROL_BACKUP_STORAGE_ROOT=/app/data/backups
EXPOSE 4190
ENTRYPOINT ["sh", "-c", "printf 'window.CONTROL_API_BASE = \"%s\";' \"${CONTROL_API_BASE:-}\" > /app/static/control-runtime-config.js && exec java -jar /app/control-service-backend.jar"]
