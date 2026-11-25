# Runbook: boletas o correos que no llegan (colas y DLQ de RabbitMQ)

## Cuándo usarlo

- Tras cerrar una planilla, `GET /api/v1/planillas/{id}/boletas/estado` queda en `EN_PROCESO` mucho tiempo, o en `CON_FALLOS`.
- Hay boletas en estado `FALLIDA` o `PENDIENTE` que no avanzan.
- Las boletas se generan pero el empleado no recibe el correo de aviso.

## Contexto (fuente: `compartido/mensajeria`, `boleta/mensajeria`, `notificacion/mensajeria`, `application.yml`)

| Cola de trabajo | Cola de mensajes muertos (DLQ) | Qué hace |
|---|---|---|
| `boletas.generar` | `boletas.generar.dlq` | Una boleta PDF por trabajador. |
| `notificaciones.correo` | `notificaciones.correo.dlq` | Aviso por correo de la boleta generada. |

- Exchange de trabajo `planilla360.trabajos`; exchange de mensajes muertos `planilla360.trabajos.dlx`.
- El consumidor reintenta 3 veces (`spring.rabbitmq.listener.simple.retry`: espera inicial 1 s, multiplicador 2, máximo 10 s). Al agotarlos, el mensaje se republica en la DLQ con el motivo en la cabecera `x-exception-message`.
- **Las DLQ tienen su propio consumidor**, así que normalmente están vacías:
  - `boletas.generar.dlq`: el consumidor registra `WARN` («Boleta del empleado ... en la DLQ») y **marca la boleta como FALLIDA** con el motivo.
  - `notificaciones.correo.dlq`: el consumidor solo registra `ERROR` («Correo de la boleta ... agotó los reintentos y queda en la DLQ»); el mensaje se consume y **no** queda guardado para reintento. El correo perdido se reenvía de forma manual (ver más abajo).
  - Un mensaje se queda visible en una DLQ solo si la API estaba apagada o sin conexión en ese momento.
- **Modo sin mensajería**: `app.messaging.enabled` vale `false` por defecto (`application.yml`; en el perfil `local` el valor por defecto es `true`, ver `application-local.yml`). Con `false` no existen colas ni DLQ: la boleta se genera en el mismo proceso y, si falla, queda `FALLIDA` sin reintentos automáticos; el correo se intenta una vez, el fallo se registra con `WARN` y no afecta a la boleta. Se controla con la variable `MESSAGING_ENABLED`.

## Diagnóstico

1. Mira el avance de la planilla (necesitas un token de RRHH o ADMIN_EMPRESA):
   ```bash
   curl -s -H "Authorization: Bearer <accessToken>" \
     http://localhost:8080/api/v1/planillas/<planillaId>/boletas/estado
   ```
   La respuesta trae `estado` (`SIN_INICIAR`, `EN_PROCESO`, `COMPLETADA`, `CON_FALLOS`) y los conteos `pendientes`, `generadas`, `enviadas`, `fallidas`.
2. Confirma qué modo corre la API: busca en el arranque la variable `MESSAGING_ENABLED` o prueba `/actuator/health` en local (con mensajería activa aparece el componente `rabbit`).
3. Si hay mensajería, revisa que RabbitMQ esté arriba:
   ```bash
   docker compose --profile messaging ps
   ```
   El servicio `rabbitmq` debe estar `healthy`. Para levantar lo necesario: `docker compose --profile messaging up -d`.
4. Abre la consola de RabbitMQ: http://localhost:15672 (usuario y clave por defecto de desarrollo en `compose.yaml`, variables `RABBITMQ_USER` y `RABBITMQ_PASSWORD`). En la pestaña **Queues** revisa las cuatro colas:
   - `Ready` alto y `Consumers` en 0: la API no está consumiendo (apagada o sin conexión). Reiníciala.
   - `Unacked` alto y fijo: consumidores atascados; mira los logs de la API.
   - Mensajes en una DLQ: abre la cola, sección *Get messages*, y lee la cabecera `x-exception-message` (usa *Nack message requeue true* para no consumirlo al mirar).
   - Colas inexistentes: la API nunca arrancó con `MESSAGING_ENABLED=true`; las colas las crea la API al conectarse.
5. Busca el motivo en los logs de la API (`WARN`/`ERROR` de `ConsumidorBoletas` y `ConsumidorCorreos`); para seguir una petición concreta usa [logs-por-traceid.md](logs-por-traceid.md). Los mensajes de las colas corren en otro hilo y no heredan el `traceId` de la petición original.
6. Causas habituales del fallo de la boleta: almacenamiento de archivos caído (MinIO, perfil `storage`), datos de la planilla inconsistentes. Del correo: servidor SMTP caído (Mailpit en local, `http://localhost:8025`) o `NOTIFICACIONES_HABILITADAS=false`.

## Mitigación y solución

1. Arregla la causa (levantar RabbitMQ, MinIO o Mailpit; reiniciar la API).
2. **Boletas**: reintenta con el endpoint de generación; solo procesa lo pendiente o fallido, así que es seguro repetirlo:
   ```bash
   curl -s -X POST -H "Authorization: Bearer <accessToken>" \
     http://localhost:8080/api/v1/planillas/<planillaId>/boletas/generar
   ```
   Responde `202` con el estado. Las boletas `FALLIDA` vuelven a `PENDIENTE` y se encolan de nuevo; las `GENERADA` y `ENVIADA` no se tocan. Solo funciona con planilla `CERRADA`.
3. **Correos**: no hay endpoint de reenvío; el aviso se emite al generarse la boleta. Si la causa estaba en SMTP y la boleta ya estaba generada, comunica a los empleados que descarguen su boleta desde la aplicación (`GET /api/v1/boletas/mias`) y escala para un reenvío manual.
4. Mensajes que quedaron en una DLQ por API apagada: al volver la API, su consumidor los procesa solo; no hace falta moverlos a mano.

## Verificar

- `GET /api/v1/planillas/<planillaId>/boletas/estado` termina en `COMPLETADA` con `fallidas` y `pendientes` en 0.
- En la consola de RabbitMQ las cuatro colas tienen `Ready` y `Unacked` en 0 (con los consumidores esperados).
- Un empleado puede descargar su PDF en `GET /api/v1/boletas/{id}/pdf`; en Mailpit aparece el correo de aviso.
- En Grafana (perfil `obs`) el panel «Colas de mensajes fallidos (DLQ)» queda en cero.

## Escalar

- La generación sigue fallando tras el reintento, o el motivo es un dato de la planilla: **responsable técnico del api**.
- Boletas o correos no entregados a empleados reales: avisa también al **responsable de producto / soporte funcional**.
- RabbitMQ no levanta o pierde datos: **responsable técnico del api**.
