# syntax=docker/dockerfile:1

#################################
# Stage 1: frontend (Tailwind CSS + Alpine.js)
#################################
FROM node:22.22-alpine AS frontend
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

# Tailwind v4's CLI auto-detects utility classes by scanning the project tree
# (there is no tailwind.config.js in this repo), so the Thymeleaf templates that
# reference Tailwind classes must be present before running the build.
COPY src ./src
RUN npm run build:frontend

#################################
# Stage 2: build the Spring Boot application with Maven Wrapper
#################################
FROM eclipse-temurin:17-jdk-alpine-3.22 AS build
WORKDIR /workspace

# Cache Maven dependencies in their own layer, independent of source changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# mvnw's executable bit is not reliably preserved by git on Windows checkouts,
# so force it here rather than depending on the file mode in the build context.
RUN chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline

# Now bring in the full source...
COPY src ./src
# ...and overlay the compiled frontend assets on top (app.css / alpine.min.js)
COPY --from=frontend /workspace/src/main/resources/static/client ./src/main/resources/static/client

# Tests need MySQL/Redis via Testcontainers (Docker-in-Docker), which this build
# stage does not have access to. Run `./mvnw test` separately in CI/locally
# (see README) before/alongside building this image; the image build packages only.
RUN ./mvnw -B -q clean package -DskipTests

#################################
# Stage 3: runtime
#################################
FROM eclipse-temurin:17-jre-alpine-3.22 AS runtime

RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads /app/seo /app/logs \
    && chown -R spring:spring /app

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

ENV UPLOAD_PATH=/app/uploads \
    SEO_STATIC_PATH=/app/seo \
    LOG_PATH=/app/logs \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

VOLUME ["/app/uploads", "/app/seo", "/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
