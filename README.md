# planilla360-api

API REST de **Planilla360**, el SaaS de RRHH y planillas de Nexo Software S.A.C. para pymes peruanas.

## Requisitos
- JDK 21 (en IntelliJ: *File → Project Structure → SDK* = 21)
- Docker Desktop (base de datos local y pruebas con Testcontainers)

## Cómo correrlo
1. Levanta PostgreSQL:
   ```bash
   docker compose --profile core up -d
   ```
2. Arranca la API (perfil `local`, también desde IntelliJ ejecutando `Planilla360ApiApplication`):
   ```bash
   ./mvnw spring-boot:run
   ```
3. Abre http://localhost:8080/swagger-ui.html, haz `POST /api/v1/auth/login` con un usuario de desarrollo y pega el `accessToken` en *Authorize*.

Salud de la aplicación: http://localhost:8080/actuator/health

### Usuarios de desarrollo (solo perfil `local`)
Contraseña común: `Planilla360-Local1`

| Rol | Correo |
|---|---|
| Admin de la plataforma | `admin.saas@nexosoftware.pe` |
| Admin de empresa (Textiles Andinos) | `admin@textilesandinos.ejemplo.pe` |
| RRHH (Textiles Andinos) | `rrhh@textilesandinos.ejemplo.pe` |
| Empleado (Textiles Andinos) | `empleado@textilesandinos.ejemplo.pe` |
| Admin / RRHH (La Espiga, otra empresa) | `admin@laespiga.ejemplo.pe` / `rrhh@laespiga.ejemplo.pe` |

### Probar las planillas
Al arrancar en `local`, cada empresa queda con seis meses de planillas cerradas (incluidas la gratificación y la CTS que caen en esa ventana). Con el token de RRHH:
1. `GET /api/v1/planillas` para ver el historial y `GET /api/v1/planillas/{id}/lineas` para ver las boletas.
2. `POST /api/v1/planillas` con `{"periodo": "2026-11"}` crea el borrador del mes en curso; `POST /api/v1/planillas/{id}/calcular` lo calcula y `GET /api/v1/planillas/{id}/validaciones` lista lo que falta para cerrarlo.
3. `POST /api/v1/planillas/{id}/cerrar` exige la cabecera `Idempotency-Key`: repetir la petición con la misma clave devuelve la misma planilla sin cerrar de nuevo.
4. El cierre exige la asistencia del mes consolidada (`POST /api/v1/asistencia/consolidaciones` con cada fecha; el seed local solo trae las dos últimas semanas) y sin solicitudes de vacaciones pendientes; si no, responde 422.
5. Anular (`POST /{id}/anular`) solo lo puede hacer el administrador de la empresa y mientras no se registre el pago (`POST /{id}/pago`).

### Perfiles de Docker Compose
| Perfil | Levanta |
|---|---|
| `core` | PostgreSQL |
| `messaging` | PostgreSQL + Redis + RabbitMQ (para usar Redis: `CACHE_TYPE=redis`) |
| `app` | PostgreSQL + la API en contenedor (`docker compose --profile app up -d --build`) |
| `full` | Todo lo disponible |

## Cómo verificarlo
```bash
./mvnw verify
```
Compila, ejecuta las pruebas (unitarias, de integración con Testcontainers, arquitectura y escenarios Cucumber), Checkstyle, Spotless, cobertura con JaCoCo (mínimo 80 % en el dominio) y mutation testing con PIT sobre el dominio. Los informes quedan en `target/site/jacoco/index.html` y `target/pit-reports/index.html`. Requiere Docker Desktop encendido.

## Perfiles de Spring
`local` (por defecto), `staging`, `prod`. La configuración sensible se entrega por variables de entorno; ver `.env.example`.

Más información: [CONTRIBUTING.md](CONTRIBUTING.md) y [CLAUDE.md](CLAUDE.md).
