# planilla360-api

API REST de **Planilla360**, el SaaS de RRHH y planillas de Nexo Software S.A.C. para pymes peruanas.

## Requisitos
- JDK 21 (en IntelliJ: *File → Project Structure → SDK* = 21)
- Docker Desktop (a partir de la fase de base de datos)

## Cómo correrlo
```bash
./mvnw spring-boot:run
```
Salud de la aplicación: http://localhost:8080/actuator/health

## Cómo verificarlo
```bash
./mvnw verify
```

## Perfiles
`local` (por defecto), `staging`, `prod`. La configuración sensible se entrega por variables de entorno; ver `.env.example`.

Más información: [CONTRIBUTING.md](CONTRIBUTING.md) y [CLAUDE.md](CLAUDE.md).
