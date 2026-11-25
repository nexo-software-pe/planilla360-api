# Runbook: seguir una petición por `traceId`

## Cuándo usarlo

Un usuario o el equipo reporta un error («me salió un 500», «el cierre falló», «tardó mucho») y hay que reconstruir qué pasó en esa petición concreta, o ver si el cálculo de planilla está lento o fallando.

## Contexto (fuente: `TraceIdFilter`, `application*.yml`, `MicrometerMedidorCalculo`, `docs/observabilidad.md`)

- Cada petición recibe un `traceId` (filtro `TraceIdFilter`, antes de la seguridad: también quedan trazados los 401 y 403). Se toma, en este orden, del `trace-id` de la cabecera W3C `traceparent`, de un `X-Request-Id` válido (8 a 64 caracteres, letras, números y guiones) o se genera uno nuevo.
- Va al MDC (aparece en todas las líneas de log de la petición) y vuelve al cliente en la cabecera de respuesta **`X-Trace-Id`**. Cuando hay autenticación, el MDC también lleva `tenantId`.
- Cada petición deja una línea de acceso: `GET /api/v1/empleados -> 200 (12 ms)`. No incluye la cadena de consulta ni cabeceras. Las sondas `/actuator/*` solo se registran en DEBUG.
- Formato de logs: **local** imprime texto con el traceId entre corchetes (`[<traceId>] ...`); **staging y prod** imprimen una línea JSON por evento (formato Logstash) con `traceId` y `tenantId` como campos.
- Los mensajes de RabbitMQ (boletas, correos) corren en otro hilo y todavía no heredan el `traceId` de la petición que los originó.

## Diagnóstico paso a paso

1. **Obtén el traceId**. Pídelo al usuario (el front lo puede mostrar junto al error) o míralo tú en la respuesta:
   ```bash
   curl -i -H "Authorization: Bearer <accessToken>" http://localhost:8080/api/v1/planillas | grep -i x-trace-id
   ```
   Para marcar tú la petición desde el origen, envía tu propio identificador:
   ```bash
   curl -i -H "Authorization: Bearer <accessToken>" -H "X-Request-Id: incidente-0001-abcdef" http://localhost:8080/api/v1/planillas
   ```
   La respuesta traerá `X-Trace-Id: incidente-0001-abcdef`.
2. **Busca en los logs**.
   - Local, API en terminal o IntelliJ: filtra la consola por el identificador.
   - Local, API en contenedor (`docker compose --profile app up -d --build`):
     ```bash
     docker compose logs api | grep "<traceId>"
     ```
   - Logs JSON (staging/prod), guardados en un archivo o exportados:
     ```bash
     grep '"traceId":"<traceId>"' api.log
     ```
     Si tienes `jq`: `jq -c 'select(.traceId=="<traceId>")' api.log`.
   - Para ver JSON en local: arranca con `LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash`.
3. **Lee la secuencia**: la línea de acceso final da método, ruta, estado y duración en ms; las líneas anteriores con el mismo traceId traen el `WARN`/`ERROR` y la traza. Una respuesta de error de negocio (4xx) normalmente no genera traza; un 500 sí.
4. **Aísla por empresa** si hace falta: filtra además por `tenantId`.
5. **Si el problema es lentitud del cálculo de planilla**, mira la métrica `planilla.calculo.duracion` (en Prometheus `planilla_calculo_duracion_seconds_*`, etiquetas `tipo` = MENSUAL, GRATIFICACION o CTS y `resultado` = `ok` o `error`). Mide el armado de las líneas, no el guardado.
   - Levanta el stack de observabilidad y la API local (perfil `local`, puerto 8080):
     ```bash
     docker compose --profile messaging --profile obs up -d
     ```
   - Grafana: http://localhost:3000 (puerto `GRAFANA_PORT`, por defecto 3000; usuario `GRAFANA_USER` y clave `GRAFANA_PASSWORD`, valores de desarrollo en `compose.yaml`). Abre el dashboard **«Planilla360 — API»** y la fila **«Cálculo de planilla»**: «Duración del cálculo (p50 / p95)», «Cálculos por minuto», «Cálculos con error» y «Duración media». Contrasta la hora del reporte con los picos.
   - Prometheus: http://localhost:9090 (puerto `PROMETHEUS_PORT`); consulta, por ejemplo, `histogram_quantile(0.95, sum by (le, tipo) (rate(planilla_calculo_duracion_seconds_bucket[10m])))`.
   - La API publica las métricas en `/actuator/prometheus` con HTTP Basic (credencial de servicio `METRICS_USER` / `METRICS_PASSWORD`); no se accede con un JWT.
   - El dashboard también tiene las filas de peticiones, errores 4xx/5xx, JVM y colas de RabbitMQ.

## Mitigación

Este runbook sirve para **diagnosticar**; la solución depende de lo que muestren los logs. Registra el traceId, el endpoint, la hora y la empresa en el ticket (nunca copies datos personales ni tokens de los logs). Si el hallazgo es de un flujo conocido, pasa al runbook correspondiente: [planilla-no-cierra.md](planilla-no-cierra.md) o [dlq-rabbitmq.md](dlq-rabbitmq.md).

## Verificar

- Con el traceId puedes reconstruir la petición completa: línea de acceso, error y traza (si existe).
- Tras la corrección, repite la petición y comprueba el nuevo `X-Trace-Id` con estado y duración esperados; en Grafana, «Cálculos con error» vuelve a cero y la latencia p95 baja.

## Escalar

- Error 5xx reproducible o con traza: **responsable técnico del api**, con el traceId, la hora y el endpoint.
- Lentitud sostenida del cálculo (p95 muy por encima de lo habitual): **responsable técnico del api**.
- Logs con datos que no deberían estar (personales, secretos): **responsable técnico del api** de inmediato.
