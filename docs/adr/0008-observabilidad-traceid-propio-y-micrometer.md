# ADR 0008: Observabilidad con traceId propio y Micrometer

- **Estado:** Aceptado
- **Fecha:** 2026-11-09 (Fase 6)

## Contexto

Hace falta poder correlacionar un error reportado por un usuario con las líneas de log de su petición, medir latencia y errores, y vigilar las colas y el cálculo de planilla. El sistema es un monolito modular con una sola API; todavía no hay otros servicios entre los que propagar trazas.

## Decisión

- **traceId propio.** `TraceIdFilter` (`compartido/infrastructure`) se ejecuta antes de Spring Security, así que incluso las respuestas 401 y 403 llevan traceId. Pone `traceId` en el MDC y lo devuelve en la cabecera `X-Trace-Id`. Respeta el `trace-id` de un `traceparent` W3C o un `X-Request-Id` solo si cumplen un formato estricto (evita inyección en los logs); si no, genera uno. Cada petición deja una línea de acceso sin cadena de consulta (puede contener datos personales). Cuando hay usuario autenticado, el MDC lleva también `tenantId`.
- **No se adopta OpenTelemetry por ahora.** Se difiere hasta que existan varios servicios que justifiquen trazas distribuidas; el formato compatible con `traceparent` facilita la migración.
- **Logs.** Texto legible con traceId en `local`; **JSON por línea** (formato Logstash de Spring Boot) en `staging` y `prod`, con `traceId` y `tenantId` como campos.
- **Métricas con Micrometer** y registro Prometheus: histograma de `http.server.requests` (p50, p95, p99), JVM, pool de conexiones y etiqueta `application`. La métrica propia `planilla.calculo.duracion` (etiquetas `tipo` y `resultado`) mide el armado del cálculo; la aplicación depende del puerto `MedidorCalculoPort` y el adaptador `MicrometerMedidorCalculo` es el único que conoce Micrometer, así el hexágono no importa la herramienta.
- **Métricas protegidas con Basic.** `/actuator/prometheus` tiene su propia cadena de seguridad con HTTP Basic y una credencial de servicio (`METRICS_USER`, `METRICS_PASSWORD`); un JWT no sirve porque caduca a los 15 minutos. Sin contraseña configurada responde siempre 401.
- **Salud.** Liveness solo mira el proceso; readiness incluye la base de datos. Redis, RabbitMQ y el correo no entran porque la API degrada sin ellos.

Detalle operativo en `docs/observabilidad.md`.

## Consecuencias

- Un traceId reportado por el cliente localiza todas las líneas de esa petición.
- Los consumidores de RabbitMQ corren en otro hilo y todavía no heredan el traceId de la petición que originó el mensaje.
- Las métricas de cola las publica RabbitMQ con su plugin de Prometheus, no la API.
- La credencial de métricas se gestiona como secreto en los entornos que la usen.
- El dominio y la aplicación no dependen de Micrometer; cambiar de herramienta de métricas toca un adaptador.

## Alternativas consideradas

- **Tracing de Micrometer/OpenTelemetry desde ya:** más completo, pero añade dependencias y un colector sin beneficio mientras haya un solo servicio.
- **JWT para el endpoint de métricas:** caduca, y un raspador necesita una credencial estable.
- **Puerto de gestión separado:** aísla las métricas, pero no todos los entornos de ejecución permiten exponer un segundo puerto.
- **Liveness con la base de datos:** una caída de PostgreSQL haría que el orquestador reinicie la API en bucle sin arreglar nada.
- **Logs en texto en todos los entornos:** difíciles de consultar por campo (`traceId`, `tenantId`).
