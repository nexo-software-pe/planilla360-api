# CLAUDE.md — planilla360-api

Backend de **Planilla360** (Nexo Software S.A.C.): SaaS multi-empresa de RRHH y planillas para pymes peruanas.

## Stack
Java 21 (fijado con maven-enforcer) · Spring Boot 4.1.x · Spring Modulith · Spring Security · Maven Wrapper.
(JPA, Flyway, PostgreSQL, JWT, Redis, RabbitMQ… se incorporan en fases siguientes.)

## Comandos
Usar JDK 21 (`JAVA_HOME` o SDK del proyecto en IntelliJ).
```
./mvnw verify          # compila, tests, Checkstyle, Spotless
./mvnw spring-boot:run # arranca con perfil local (puerto 8080)
```
Desde PowerShell: `.\mvnw.cmd verify`. Si falla, usar la terminal de Git Bash.

## Convenciones
- **Dominio en español** (clases, métodos de negocio, eventos, tablas, API). **Infraestructura en inglés** (config, filtros, adapters técnicos).
- Paquete base `pe.nexosoftware.planilla360`; un paquete raíz por módulo (Spring Modulith). Módulos solo se hablan por su API pública o eventos.
- Núcleo (planilla, asistencia, vacaciones): hexagonal `domain/ application/ infrastructure/`. Soporte: capas clásicas.
- Commits: Conventional Commits. Ramas: `feat/<n>-...`, `fix/<n>-...`, PR a `staging`.
- **Ningún secreto en el repo**: usar variables de entorno; `.env.example` documenta las variables.

## Dónde vive qué
- `src/main/java/.../Planilla360ApiApplication.java` — arranque.
- `src/main/java/.../compartido/infrastructure/SecurityConfig.java` — seguridad base.
- `src/main/resources/application*.yml` — configuración por perfil (`local`, `staging`, `prod`).
- `config/checkstyle/checkstyle.xml` — reglas de estilo.
- `.github/workflows/ci.yml` — CI.

## Estado
Fase 1: esqueleto con CI. Módulos de negocio pendientes.
