# ===========================
# PetsApp Backend — Dockerfile (multi-stage)
# ===========================
# Stage 1: Build JAR z Maven
# Stage 2: Runtime (tylko JRE, minimalny obraz)
#
# Build lokalnie:
#   docker build -t petsapp-backend .
# Run lokalnie:
#   docker run -p 8080:8080 --env-file .env petsapp-backend

# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Najpierw kopiuj pom.xml i pobierz zależności (warstwy Docker cache)
COPY pom.xml .
RUN mvn -f pom.xml dependency:go-offline -B 2>/dev/null || true

# Skopiuj source i zbuduj
COPY src ./src
COPY mvnw* ./
COPY .mvn ./.mvn

# Jeśli nie ma mvnw, użyj systemowego mvn
RUN if [ -f mvnw ]; then \
      chmod +x mvnw && ./mvnw clean package -DskipTests -B; \
    else \
      apk add --no-cache maven && mvn clean package -DskipTests -B; \
    fi

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime

# Użytkownik non-root (bezpieczeństwo)
RUN addgroup -S petsapp && adduser -S petsapp -G petsapp
USER petsapp

WORKDIR /app

# Kopiuj JAR z build stage
COPY --from=builder /workspace/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check (Railway i inne platformy używają tego)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/health || exit 1

# Uruchomienie z profilem prod
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
  "-jar", "app.jar"]
