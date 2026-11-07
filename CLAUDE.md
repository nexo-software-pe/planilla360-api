# CLAUDE.md — planilla360-api

Backend de **Planilla360** (Nexo Software S.A.C.): SaaS multi-empresa de RRHH y planillas para pymes peruanas.

## Stack
Java 21 (fijado con maven-enforcer) · Spring Boot 4.1.x · Spring Modulith · Spring Security (resource server JWT) · Spring Data JPA + Hibernate 7 · Flyway · PostgreSQL · Redis (caché) · MapStruct · springdoc · Bucket4j · Maven Wrapper.
Pruebas: JUnit 5, AssertJ, Mockito, spring-security-test, Testcontainers (PostgreSQL, Redis), ArchUnit, Spring Modulith.
(RabbitMQ, MinIO, Mailpit y observabilidad completa se incorporan en fases siguientes.)

## Comandos
Usar JDK 21 (`JAVA_HOME` o SDK del proyecto en IntelliJ). Los tests de integración necesitan Docker Desktop encendido.
```
docker compose --profile core up -d   # PostgreSQL local (messaging = + Redis y RabbitMQ; app = API en contenedor)
./mvnw spring-boot:run                # perfil local, puerto 8080, datos ficticios de desarrollo
./mvnw verify                         # compila, tests, Checkstyle, Spotless, JaCoCo (≥ 80 %) y PIT (dominio)
./mvnw test -Dtest='ValidadorCierreTests*'   # solo unas pruebas (las clases @Nested llevan * al final)
```
Informes: `target/site/jacoco/index.html` (cobertura) y `target/pit-reports/index.html` (mutación). Escenarios Cucumber: `EscenariosPlanillaTests`.
Desde PowerShell: `.\mvnw.cmd verify`. Si falla, usar la terminal de Git Bash.
Swagger (solo local/staging): http://localhost:8080/swagger-ui.html — botón *Authorize* con el `accessToken` del login.

## Convenciones
- **Dominio en español** (clases, métodos de negocio, eventos, tablas, API). **Infraestructura en inglés** (config, filtros, adapters técnicos, sufijos `Controller`/`Service`/`Repository`).
- Paquete base `pe.nexosoftware.planilla360`; un paquete raíz por módulo (Spring Modulith). Un módulo solo usa la **API pública** de otro (las clases de su paquete raíz) o sus **eventos**; el interior (`controller/service/repository/entity/dto/mapper`) es privado.
- Núcleo (planilla, asistencia, vacaciones): hexagonal `domain/ application/ infrastructure/`. Soporte: capas clásicas. `ArquitecturaTests` (ArchUnit) hace cumplir las reglas.
- Controllers: reciben/devuelven **DTO** (records), nunca entidades; validación con Bean Validation; autorización con `@PreAuthorize` (roles `ADMIN_SAAS`, `ADMIN_EMPRESA`, `RRHH`, `EMPLEADO`). Listados paginados con `Pagina<T>` y `?page=&size=&sort=`.
- Errores: se lanzan excepciones de `compartido.excepcion` (`RecursoNoEncontrado`, `Conflicto`, `ReglaDeNegocio`…); `GlobalExceptionHandler` las convierte a `application/problem+json`.
- **Multi-tenant por columna `tenant_id`**: el tenant sale del JWT (`TenantContextFilter` → `TenantContext`). Las entidades de negocio llevan `@TenantId` de Hibernate (filtra lecturas y asigna el tenant al guardar). `Usuario` y `AuditoriaCambio` no (el tenant puede ser nulo): sus servicios filtran explícitamente. Un recurso de otra empresa se responde **404**. Las consultas SQL nativas NO se filtran solas: incluir `tenant_id` a mano.
- Auditoría: `created_at/by`, `updated_at/by` con `AuditableEntity`; para el historial, publicar `CambioRegistrado` (lo guarda el módulo `auditoria` en la misma transacción). Nunca incluir contraseñas en las instantáneas.
- Parámetros legales (RMV, UIT, tasas) se leen de `CatalogoApi.valorParametro(codigo, fecha)`; nunca se escriben en el código.
- **TDD en el dominio**: primero el test en rojo (commit `test:` con un esqueleto que compila y lanza `UnsupportedOperationException`), luego la implementación (`feat:`) y, si hace falta, el `refactor:`. Los tests del dominio van en paquetes `..domain..` / `compartido.dominio` porque PIT solo ejecuta esos. Un cambio en el dominio no se da por bueno si baja el PIT por debajo de 85 % o la cobertura por debajo de 80 %.
- Dinero siempre con `Dinero` (nunca `double`). Fechas: `LocalDate`/`Periodo`; el dominio no llama a `LocalDate.now()` (la fecha se recibe como dato).
- Commits: Conventional Commits. Ramas: `feat/<n>-...`, `fix/<n>-...`, PR a `staging`.
- **Ningún secreto en el repo**: variables de entorno; `.env.example` documenta las variables. En local la API genera un `JWT_SECRET` efímero si no se define.

## Dónde vive qué
- `compartido/` (módulo abierto): `tenant/TenantContext`, `seguridad/` (Rol, UsuarioAutenticado), `excepcion/`, `dominio/` (Ruc, Dni), `validacion/`, `persistencia/AuditableEntity`, `paginacion/Pagina`, `eventos/CambioRegistrado`.
- `compartido/infrastructure/` — `SecurityConfig` (cadena de filtros), `JwtConfig`, `GlobalExceptionHandler`, `HibernateTenantConfig`, `CorsConfig`, `OpenApiConfig`, `JpaAuditingConfig`, `CacheConfig`.
- `empresa/` — empresas cliente (`/api/v1/empresas`, solo ADMIN_SAAS). API pública: `EmpresaApi`.
- `identidad/` — login, refresh con rotación, logout, perfil, cambio de contraseña (`/api/v1/auth`) y usuarios (`/api/v1/usuarios`). Rate limit de login con Bucket4j.
- `catalogo/` — áreas y cargos (por empresa), AFP, parámetros legales versionados y feriados (globales, con caché). API pública: `CatalogoApi`.
- `auditoria/` — historial de cambios (`/api/v1/auditoria`).
- `compartido/dominio/` — value objects `Dinero` (BigDecimal, 2 decimales, redondeo HALF_UP, `proporcional` redondea una sola vez) y `Periodo` (YearMonth), además de `Dni` y `Ruc`.
- `planilla/domain/` (núcleo hexagonal, **Java puro**; aún sin `application/` ni `infrastructure/`, llegan en la Fase 5):
  - `model/` — `Planilla` (agregado; `cerrar` es idempotente por clave y devuelve el evento `PlanillaCerrada`), `EstadoPlanilla` (State: BORRADOR → CALCULADA → CERRADA, + ANULADA), `LineaPlanilla` + `Builder` (boleta: totales, neto y `conceptos()`), `ParametrosLegales` (RMV, UIT y tasas llegan como datos, nunca hardcodeados), `Gratificacion`, `DepositoCts`.
  - `regimen/` — `RegimenPensionario` (Strategy sellada: `Onp`, `Afp`) + `FabricaRegimenPensionario` (Factory); `RegimenLaboral` (general / pequeña empresa).
  - `service/` — calculadoras sin estado: asignación familiar, tardanzas e inasistencias, gratificación, CTS y renta de 5.ª categoría (simplificada: impuesto anual ÷ 12).
  - `validation/` — cadena de validaciones de cierre (`ValidadorCierre.estandar()`): reúne **todas** las incidencias, no corta en la primera.
  - `excepcion/ReglaDePlanillaException` — error de negocio del dominio (sin Spring); la capa web la traducirá a `problem+json`.
- Pruebas del dominio: `src/test/.../planilla/domain/**` (unitarias, AssertJ, parametrizadas; `ParametrosLegalesBuilder` y `LineasDePrueba` son test data builders) y escenarios Gherkin en español en `src/test/resources/features/planilla/*.feature` con sus pasos en `planilla/bdd/`.
- `src/main/resources/db/migration` — Flyway (`V1` esquema, `V2` datos referenciales). `db/seed/local` — datos ficticios, solo perfil `local`.
- `src/main/resources/application*.yml` — configuración por perfil (`local`, `staging`, `prod`); `src/test/resources/application-test.yml`.
- `src/test/.../soporte` — `IntegracionBase` (app completa + PostgreSQL real), `TestcontainersConfig`.
- `Dockerfile`, `compose.yaml` — imagen multi-etapa (sin root) y servicios locales por perfiles.
- `config/checkstyle/checkstyle.xml` — reglas de estilo. `.github/workflows/ci.yml` — CI.

## Usuarios de desarrollo (perfil local)
Contraseña común: `Planilla360-Local1` (solo local; en staging/prod los usuarios se crean fuera del repo).
`admin.saas@nexosoftware.pe` (ADMIN_SAAS) · `admin@textilesandinos.ejemplo.pe` (ADMIN_EMPRESA) · `rrhh@textilesandinos.ejemplo.pe` (RRHH) · `empleado@textilesandinos.ejemplo.pe` (EMPLEADO) · `admin@laespiga.ejemplo.pe` y `rrhh@laespiga.ejemplo.pe` (segunda empresa, para probar aislamiento).

## Estado
Fase 2 cerrada: backend base y seguridad. Fase 3: dominio de planilla (cálculo y cierre) con pruebas, Cucumber, JaCoCo y PIT. Pendiente: empleados, asistencia, vacaciones, aplicación e infraestructura de planilla, boletas, mensajería, observabilidad y despliegue.
