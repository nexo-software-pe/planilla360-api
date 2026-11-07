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

## Perfiles de Spring
`local` (por defecto), `staging`, `prod`. La configuración sensible se entrega por variables de entorno; ver `.env.example`.

Más información: [CONTRIBUTING.md](CONTRIBUTING.md) y [CLAUDE.md](CLAUDE.md).
