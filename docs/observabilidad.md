# Observabilidad

Cómo saber qué está haciendo la API: salud, métricas, logs y el panel de Grafana.

## Salud (Actuator)

| Endpoint | Acceso | Para qué |
|---|---|---|
| `/actuator/health/liveness` | público | ¿El proceso está vivo? Solo mira su propio estado, **nunca** la base de datos: si falla, el orquestador reinicia el contenedor. |
| `/actuator/health/readiness` | público | ¿Puede recibir tráfico? Incluye la base de datos: si PostgreSQL no responde, sale de rotación sin reiniciarse. |
| `/actuator/health` | público | Resumen. Los detalles (`db`, `rabbit`, …) solo se muestran en el perfil `local`. |
| `/actuator/prometheus` | HTTP Basic | Métricas en formato Prometheus (ver abajo). |

Por qué dos sondas: si liveness dependiera de la base de datos, una caída de PostgreSQL haría que el orquestador reiniciara la API en bucle sin arreglar nada. Redis, RabbitMQ y el correo no entran en readiness porque la API degrada sin ellos (ver «Interruptores» del README).

## Métricas (Micrometer)

Micrometer recoge las métricas y el registro de Prometheus las publica en `/actuator/prometheus`. Todas llevan la etiqueta `application="planilla360-api"`.

- **HTTP**: `http_server_requests_seconds_*` (peticiones, latencia con histograma, por método, ruta-plantilla, estado y resultado).
- **JVM**: memoria, GC, hilos, CPU (`jvm_*`, `process_*`, `system_*`), y el pool de conexiones (`hikaricp_*`).
- **Cálculo de planilla**: `planilla_calculo_duracion_seconds_*`, con las etiquetas `tipo` (MENSUAL, GRATIFICACION, CTS) y `resultado` (`ok`/`error`). Mide el armado de las líneas (lectura de datos de otros módulos y cálculo), no el guardado. Se define detrás del puerto `MedidorCalculoPort`; el dominio y la aplicación no conocen Micrometer.
- **Colas de RabbitMQ**: no las publica la API sino RabbitMQ con su plugin `rabbitmq_prometheus`; Prometheus las raspa directamente (`rabbitmq_detailed_queue_messages_ready`, `…_unacked`, `…_consumers`, por cola).

### Acceso al endpoint de métricas

`/actuator/prometheus` usa **HTTP Basic con una credencial de servicio** (`app.metrics.user` / `app.metrics.password`, variables `METRICS_USER` / `METRICS_PASSWORD`), no un JWT: un JWT de usuario caduca a los 15 minutos y un raspador necesita una credencial estable. Un JWT de administrador **no** da acceso.

- `local`: la contraseña por defecto es `planilla360-metrics-local` (la misma que lee el Prometheus de compose).
- `staging` y `prod`: sin `METRICS_PASSWORD` el endpoint responde siempre `401`. En Render el endpoint no se necesita (se usan los logs y las sondas); si algún día se conecta un Prometheus externo, se define la variable como secreto.

## Logs

- Toda petición recibe un **`traceId`** (`TraceIdFilter`): se respeta el `trace-id` de la cabecera W3C `traceparent` o un `X-Request-Id` válido; si no, se genera uno. Está en el MDC (todas las líneas de log de esa petición) y vuelve al cliente en la cabecera **`X-Trace-Id`**: cuando alguien reporta un error, ese valor localiza todo lo ocurrido. Lo que no cumpla el formato se descarta (no se puede inyectar texto en los logs).
- Cada petición deja una línea de acceso: `GET /api/v1/empleados -> 200 (12 ms)`. No incluye la cadena de consulta ni cabeceras (pueden llevar datos personales). Las sondas `/actuator/*` solo se registran en DEBUG.
- Cuando la petición está autenticada, el MDC también lleva `tenantId` (la empresa).
- **Formato**: `local` imprime texto legible con el traceId entre corchetes. `staging` y `prod` imprimen **una línea JSON por evento** (formato Logstash, `logging.structured.format.console`), con `traceId` y `tenantId` como campos. Para ver JSON en local: `LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash` (variable de entorno) o `-Dlogging.structured.format.console=logstash`.

Ejemplo de línea en staging/prod:

```json
{"@timestamp":"2026-11-09T01:20:11.482-05:00","message":"GET /api/v1/planillas -> 200 (37 ms)","logger_name":"...TraceIdFilter","level":"INFO","traceId":"4bf92f3577b34da6a3ce929d0e0e4736"}
```

Los mensajes que consumen RabbitMQ (boletas, correos) corren en otro hilo y todavía no heredan el `traceId` de la petición que los originó: la trazabilidad entre servicios llegará con OpenTelemetry.

## Prometheus y Grafana en local

```bash
docker compose --profile messaging --profile storage --profile obs up -d   # o: --profile full
```

Con la API corriendo (IntelliJ, perfil `local`, puerto 8080):

| Servicio | URL | Acceso |
|---|---|---|
| Grafana | http://localhost:3000 | `admin` / `planilla360-local` (`GRAFANA_USER`, `GRAFANA_PASSWORD`) |
| Prometheus | http://localhost:9090 | sin login (solo `127.0.0.1`) |

Grafana arranca con la fuente de datos y el dashboard **«Planilla360 — API»** ya provisionados (`docker/grafana/`): resumen (API arriba, req/s, % de 5xx, latencia p95, heap), peticiones por estado y por endpoint, latencia p50/p95/p99, errores 4xx/5xx, JVM (heap, GC, hilos, CPU, Hikari), colas de RabbitMQ (listos, sin confirmar, DLQ, consumidores) y duración del cálculo de planilla. El dashboard es de solo lectura (`allowUiUpdates: false`); para cambiarlo, edita `docker/grafana/dashboards/planilla360.json` y reinicia Grafana.

Prometheus alcanza la API por `host.docker.internal:8080` (la API corre fuera de Docker). Si usas el perfil `app` (API en contenedor), el puerto 8080 también está publicado en el host, así que funciona igual. Para ver datos de RabbitMQ hace falta el perfil `messaging` (o `full`) junto con `obs`.

Las imágenes están fijadas a una versión: `prom/prometheus:v3.16.0` y `grafana/grafana:13.2.5` (en `compose.yaml`). Para actualizar, cambia la etiqueta, levanta y revisa que el dashboard siga mostrando datos.

Para empezar de cero (borra métricas e historial de Grafana): `docker compose --profile messaging --profile storage --profile obs down -v`.

## Qué se dejó para después

Trazas distribuidas con OpenTelemetry, alertas y un runbook (épica [J] del brief): cuando exista el microservicio de notificaciones.
