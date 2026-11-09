# planilla360-api

API REST de **Planilla360**, el SaaS de RRHH y planillas de Nexo Software S.A.C. para pymes peruanas.

## Requisitos
- JDK 21 (en IntelliJ: *File → Project Structure → SDK* = 21)
- Docker Desktop (base de datos local y pruebas con Testcontainers)

## Cómo correrlo
1. Levanta la infraestructura local (PostgreSQL, Redis, RabbitMQ, MinIO, Mailpit y WireMock):
   ```bash
   docker compose --profile full up -d
   ```
   Si solo quieres la base de datos: `docker compose --profile core up -d` y arranca con la mensajería apagada (`MESSAGING_ENABLED=false STORAGE_TIPO=memoria INTEGRACION_MODO=stub`, ver más abajo).
2. Arranca la API (perfil `local`, también desde IntelliJ ejecutando `Planilla360ApiApplication`):
   ```bash
   ./mvnw spring-boot:run
   ```
3. Abre http://localhost:8080/swagger-ui.html, haz `POST /api/v1/auth/login` con un usuario de desarrollo y pega el `accessToken` en *Authorize*.

Salud de la aplicación: http://localhost:8080/actuator/health

### Servicios locales
| Servicio | Dirección | Para qué |
|---|---|---|
| API / Swagger | http://localhost:8080/swagger-ui.html | la aplicación |
| Mailpit | http://localhost:8025 | bandeja donde llegan los correos (avisos de boleta) |
| MinIO | http://localhost:9001 (usuario `planilla360`, clave `planilla360-local`) | PDF de las boletas, bucket `planilla360-boletas` (se crea solo) |
| RabbitMQ | http://localhost:15672 (usuario `planilla360`, clave `planilla360`) | colas `boletas.generar`, `notificaciones.correo` y sus DLQ |
| WireMock | http://localhost:8099 | servicio externo simulado de DNI, RUC y tipo de cambio (`docker/wiremock/mappings`) |

Los puertos escuchan solo en `127.0.0.1`. Si otro programa usa uno (por ejemplo, el 8099 de WireMock), cámbialo en `.env` (`WIREMOCK_PORT`) y ajusta `INTEGRACION_BASE_URL`.

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

### Otras funciones de la Fase 5b
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
| `full` | Todo lo anterior, salvo la API |

La imagen de MinIO es la de Chainguard (`cgr.dev/chainguard/minio`, fijada por digest en `compose.yaml`): MinIO dejó de publicar imágenes oficiales en Docker Hub y Quay.

## Cómo verificarlo
```bash
./mvnw verify
```
Compila, ejecuta las pruebas (unitarias, de integración con Testcontainers, arquitectura y escenarios Cucumber), Checkstyle, Spotless, cobertura con JaCoCo (mínimo 80 % en el dominio) y mutation testing con PIT sobre el dominio. Los informes quedan en `target/site/jacoco/index.html` y `target/pit-reports/index.html`. Requiere Docker Desktop encendido.

## Perfiles de Spring
`local` (por defecto), `staging`, `prod`. La configuración sensible se entrega por variables de entorno; ver `.env.example`.

Más información: [CONTRIBUTING.md](CONTRIBUTING.md) y [CLAUDE.md](CLAUDE.md).
