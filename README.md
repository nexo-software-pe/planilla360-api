# planilla360-api

API REST de **Planilla360**, el SaaS multi-empresa de RRHH y planillas de Nexo Software S.A.C. para pymes peruanas. Gestiona empresas cliente, empleados, asistencia, vacaciones, planillas (mensual, gratificación y CTS), boletas en PDF con aviso por correo, importación desde Excel y reportes. Cada empresa ve solo sus datos (multi-tenant por columna `tenant_id`).

## Stack
Java 21 · Spring Boot 4.1 · Spring Modulith · Spring Security (JWT) · Spring Data JPA + Flyway · PostgreSQL · Redis · RabbitMQ · MinIO (S3) · Maven Wrapper. Pruebas con JUnit 5, Testcontainers, ArchUnit, Cucumber y PIT. El detalle completo está en [CLAUDE.md](CLAUDE.md).

## Requisitos
- **Git**.
- **JDK 21** (LTS). El build falla con otra versión (maven-enforcer). Si tu `java` por defecto es otro (por ejemplo 26), apunta `JAVA_HOME` al 21 antes de usar `./mvnw`:
  - Git Bash: `export JAVA_HOME="/c/Program Files/Java/jdk-21"`
  - PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"`

  Ajusta la ruta a donde tengas instalado el JDK 21. Compruébalo con `./mvnw -v` (debe decir `Java version: 21...`).
- **Docker Desktop** encendido (base de datos local y pruebas con Testcontainers). Compruébalo con `docker info`.

## Inicio rápido
1. Clona el repositorio y entra en él:
   ```bash
   git clone https://github.com/nexo-software-pe/planilla360-api.git
   cd planilla360-api
   ```
2. Levanta la infraestructura local (PostgreSQL, Redis, RabbitMQ, MinIO, Mailpit, WireMock, Prometheus y Grafana). No necesita ningún archivo `.env`: los valores por defecto sirven para desarrollo.
   ```bash
   docker compose --profile full up -d
   ```
   Si solo quieres la base de datos: `docker compose --profile core up -d` y arranca la API con la mensajería apagada (`MESSAGING_ENABLED=false STORAGE_TIPO=memoria INTEGRACION_MODO=stub`, ver «Interruptores»).
3. Arranca la API (perfil `local`, el predeterminado). En el primer arranque Flyway crea el esquema y carga los datos ficticios.
   - Git Bash: `./mvnw spring-boot:run`
   - PowerShell: `.\mvnw.cmd spring-boot:run` (si falla, usa Git Bash)
   - IntelliJ: abre la carpeta como proyecto Maven, *File → Project Structure → SDK* = 21 y ejecuta `Planilla360ApiApplication` (el perfil `local` se aplica solo).
4. Abre http://localhost:8080/swagger-ui.html, ejecuta `POST /api/v1/auth/login` con un usuario de desarrollo (ver más abajo; por ejemplo `rrhh@textilesandinos.ejemplo.pe` y la contraseña común) y copia el `accessToken` de la respuesta. Pulsa *Authorize*, pégalo y ya puedes probar el resto de endpoints.

Salud de la aplicación: http://localhost:8080/actuator/health

### Servicios locales
| Servicio | Dirección | Para qué |
|---|---|---|
| API / Swagger | http://localhost:8080/swagger-ui.html | la aplicación |
| Mailpit | http://localhost:8025 | bandeja donde llegan los correos (avisos de boleta) |
| MinIO | http://localhost:9001 (usuario `planilla360`, clave `planilla360-local`) | PDF de las boletas, bucket `planilla360-boletas` (se crea solo) |
| RabbitMQ | http://localhost:15672 (usuario `planilla360`, clave `planilla360`) | colas `boletas.generar`, `notificaciones.correo` y sus DLQ |
| Grafana | http://localhost:3000 (usuario `admin`, clave `planilla360-local`) | dashboard de la API (perfil `obs`) |
| Prometheus | http://localhost:9090 | métricas (perfil `obs`) |
| WireMock | http://localhost:8099 | servicio externo simulado de DNI, RUC y tipo de cambio (`docker/wiremock/mappings`) |

PostgreSQL escucha en `127.0.0.1:5432`. Los puertos escuchan solo en `127.0.0.1`. Si otro programa usa uno (por ejemplo, el 8099 de WireMock), cámbialo en `.env` (copia de `.env.example`; variable `WIREMOCK_PORT`) y ajusta `INTEGRACION_BASE_URL`. Las claves de esta tabla son solo para tu máquina.

### Reiniciar la base local desde cero
Los datos ficticios (empleados, seis meses de asistencia y planillas) se cargan al arrancar la primera vez y quedan **anclados a la fecha de ese día**. Para empezar de cero, o para que el seed se vuelva a calcular con la fecha de hoy (conviene hacerlo al cambiar de mes):
```bash
# 1. Detén la API (Ctrl+C o el botón de IntelliJ).
# 2. Borra contenedores Y volúmenes (base de datos, colas y archivos de MinIO). Usa el mismo perfil con el que levantaste.
docker compose --profile full down -v
# 3. Vuelve a levantar y arranca la API: Flyway recrea el esquema y el seed.
docker compose --profile full up -d
./mvnw spring-boot:run
```
`down -v` borra **todo** lo local: no hay nada que conservar ahí. Sin la opción `-v` los datos se mantienen. Ojo: el `down` sin `--profile` no detiene los servicios que pertenecen a un perfil.

### Usuarios de desarrollo (solo perfil `local`)
Contraseña común: `Planilla360-Local1`

| Rol | Correo |
|---|---|
| Admin de la plataforma | `admin.saas@nexosoftware.pe` |
| Admin de empresa (Textiles Andinos) | `admin@textilesandinos.ejemplo.pe` |
| RRHH (Textiles Andinos) | `rrhh@textilesandinos.ejemplo.pe` |
| Empleado (Textiles Andinos) | `empleado@textilesandinos.ejemplo.pe` |
| Admin / RRHH (La Espiga, otra empresa) | `admin@laespiga.ejemplo.pe` / `rrhh@laespiga.ejemplo.pe` |

### Datos de ejemplo (seed local)
- Dos empresas con sus áreas, cargos, contratos y horarios. Cada empleado tiene **asistencia completa de los seis meses anteriores y del mes en curso** (marcaciones y asistencia consolidada con tardanzas, horas extra aprobadas, faltas, feriados y descansos), generada con las mismas reglas del consolidador.
- Cada empresa tiene seis meses de planillas **cerradas con las mismas validaciones que exige el sistema** (incluidas la gratificación de julio y la CTS de mayo o noviembre cuando caen en la ventana). Esas planillas históricas no generan boletas ni correos.
- La planilla mensual del **mes en curso queda en borrador**, lista para calcularla y cerrarla.
- Para poder cerrar el mes en una demostración, el seed deja consolidados **también los días del mes que aún no ocurrieron** (en un sistema real el último día se consolida al terminar). Datos de prueba, solo en `local`.

### Probar el flujo completo: planilla → boletas → correo
Con el token de RRHH (`rrhh@textilesandinos.ejemplo.pe`):
1. `GET /api/v1/planillas?periodo=2026-11&tipo=MENSUAL` devuelve el borrador del mes (ajusta el periodo al mes en curso).
2. `POST /api/v1/planillas/{id}/calcular` y `GET /api/v1/planillas/{id}/validaciones` (debe salir `valida: true`).
3. `POST /api/v1/planillas/{id}/cerrar` con la cabecera `Idempotency-Key` (repetir la petición con la misma clave devuelve la misma planilla con `Idempotent-Replayed: true`).
4. El cierre encola una boleta por trabajador en RabbitMQ. Sigue el avance en `GET /api/v1/planillas/{id}/boletas/estado` (`EN_PROCESO` → `COMPLETADA`).
5. `GET /api/v1/planillas/{id}/boletas` lista las boletas y `GET /api/v1/boletas/{id}/pdf` descarga el PDF (guardado en MinIO). El empleado ve las suyas en `GET /api/v1/boletas/mias`.
6. Cada trabajador recibe un correo de aviso; ábrelos en http://localhost:8025.
7. Si una boleta falla tras los reintentos, queda `FALLIDA` (y su mensaje en la DLQ); `POST /api/v1/planillas/{id}/boletas/generar` vuelve a procesar solo lo pendiente o fallido.

Anular (`POST /{id}/anular`) solo lo puede hacer el administrador de la empresa y mientras no se registre el pago (`POST /{id}/pago`).

### Otras funciones
- **Importación de empleados desde Excel:** `GET /api/v1/importaciones/empleados/plantilla.xlsx` baja la plantilla; `POST /api/v1/importaciones/empleados` (multipart `archivo`) valida fila por fila y por defecto solo simula (`simular=true`); con `simular=false` guarda las filas válidas.
- **Reportes** (`/api/v1/reportes`): `tardanzas-por-area`, `costo-laboral`, `vacaciones-pendientes` y `planilla-mensual.xlsx`, con SQL nativo.
- **Consultas externas** (`/api/v1/consultas`): `dni/{dni}`, `ruc/{ruc}` y `tipo-cambio`, con reintentos y cortacircuito (Resilience4j). Para ver el cortacircuito: `docker compose stop wiremock` y repite la consulta (503); al reanudarlo se recupera.

### Interruptores (degradar sin servicios)
| Variable | Valor | Efecto |
|---|---|---|
| `MESSAGING_ENABLED` | `true` (local) / `false` | con `false` no hace falta RabbitMQ: las boletas y los correos se procesan en el mismo proceso, sin reintentos ni DLQ |
| `STORAGE_TIPO` | `s3` (local: MinIO; nube: R2) / `memoria` | `memoria` no necesita MinIO, pero los PDF se pierden al reiniciar |
| `NOTIFICACIONES_HABILITADAS` | `true` (local) / `false` | con `false` no se envían correos |
| `INTEGRACION_MODO` | `http` (local: WireMock) / `stub` | `stub` responde datos ficticios sin servicio externo |

### Perfiles de Docker Compose
| Perfil | Levanta |
|---|---|
| `core` | PostgreSQL |
| `messaging` | PostgreSQL + Redis + RabbitMQ (para usar Redis: `CACHE_TYPE=redis`) |
| `storage` | PostgreSQL + MinIO + Mailpit + WireMock |
| `app` | PostgreSQL + la API en contenedor (`docker compose --profile app up -d --build`) |
| `obs` | Prometheus + Grafana (se combina con los demás: `--profile messaging --profile obs`) |
| `full` | Todo lo anterior (incluido `obs`), salvo la API |

La imagen de MinIO es la de Chainguard (`cgr.dev/chainguard/minio`, fijada por digest en `compose.yaml`): MinIO dejó de publicar imágenes oficiales en Docker Hub y Quay.

### Observabilidad
Salud (`/actuator/health/liveness` y `/readiness`), métricas con Micrometer, logs con `traceId` por petición y un dashboard de Grafana ya provisionado (Grafana http://localhost:3000, Prometheus http://localhost:9090; ambos arrancan con `--profile full` o `--profile obs`). Todo el detalle en [docs/observabilidad.md](docs/observabilidad.md).

### Contrato de la API
El contrato OpenAPI está versionado en [docs/openapi/openapi.json](docs/openapi/openapi.json). Si cambias un endpoint o un DTO, regenéralo (`./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true`); el CI falla si el cambio rompe `/api/v1`. Ver [docs/contrato-api.md](docs/contrato-api.md).

## Pruebas
Requieren Docker Desktop encendido y `JAVA_HOME` en el JDK 21.
```bash
./mvnw verify                                  # todo: tests, Checkstyle, Spotless, JaCoCo y PIT
./mvnw test -Dtest='ValidadorCierreTests*'     # solo unas pruebas (las clases @Nested llevan * al final)
./mvnw test -Dtest=ContratoOpenApiTests        # solo el contrato OpenAPI
```
En PowerShell usa `.\mvnw.cmd` y deja las comillas simples en `-Dtest`. `verify` compila, ejecuta las pruebas (unitarias, de integración con Testcontainers, arquitectura, contrato OpenAPI y escenarios Cucumber), Checkstyle, Spotless, cobertura con JaCoCo (mínimo 80 % en el dominio) y mutation testing con **PIT** sobre el dominio (mínimo 85 %; corre dentro de `verify`). Informes: `target/site/jacoco/index.html` y `target/pit-reports/index.html`.

## Perfiles de Spring
`local` (por defecto), `staging`, `prod`. La configuración sensible se entrega por variables de entorno; ver `.env.example`. En local no hace falta definir nada: la API genera un `JWT_SECRET` efímero (los tokens dejan de valer al reiniciar).

## Estructura del código
Monolito modular (Spring Modulith) en `src/main/java/pe/nexosoftware/planilla360`, un paquete por módulo:

| Módulo | Responsabilidad |
|---|---|
| `compartido` | tenant, seguridad, excepciones, value objects (`Dinero`, `Periodo`), filtros y configuración |
| `empresa`, `identidad`, `catalogo`, `auditoria` | empresas cliente, login y usuarios, catálogos y parámetros legales, historial de cambios |
| `empleado` | empleados, contratos y familiares |
| `asistencia`, `vacaciones`, `planilla` | núcleo del negocio (hexagonal: `domain/`, `application/`, `infrastructure/`) |
| `boleta`, `notificacion` | boletas PDF, almacenamiento y correos, con RabbitMQ |
| `importacion`, `reporte`, `integracion` | Excel, reportes con SQL nativo y consultas externas (DNI, RUC, tipo de cambio) |

Las migraciones están en `src/main/resources/db/migration` y los datos ficticios en `db/seed/local`. El mapa detallado de «dónde vive qué» está en [CLAUDE.md](CLAUDE.md).

## Mapa de documentación
| Documento | Contenido |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | flujo de trabajo, ramas, commits y definición de hecho |
| [CLAUDE.md](CLAUDE.md) | convenciones, mapa del código y usuarios de desarrollo |
| [docs/observabilidad.md](docs/observabilidad.md) | métricas, logs con `traceId`, Prometheus y Grafana |
| [docs/contrato-api.md](docs/contrato-api.md) | contrato OpenAPI versionado y reglas de compatibilidad |
| [docs/openapi/openapi.json](docs/openapi/openapi.json) | el contrato de la API |
| `.env.example` | variables de entorno disponibles |
