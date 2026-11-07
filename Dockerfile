# syntax=docker/dockerfile:1
# Imagen multi-etapa: compila con JDK 21 y ejecuta con un JRE 21 mínimo, sin root.

# ---- Etapa 1: compilación ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Primero lo que cambia poco (wrapper y pom) para aprovechar la caché de capas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline

COPY config/ config/
COPY src/ src/
# Las pruebas y el estilo ya los valida el CI; aquí solo se empaqueta.
RUN ./mvnw -B -ntp -q package -DskipTests -Dcheckstyle.skip -Dspotless.check.skip \
    && java -Djarmode=tools -jar target/planilla360-api-*.jar extract --layers --destination /workspace/extracted

# ---- Etapa 2: ejecución ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Capas de menos a más volátiles: las dependencias casi nunca cambian entre builds.
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "planilla360-api-0.1.0-SNAPSHOT.jar"]
