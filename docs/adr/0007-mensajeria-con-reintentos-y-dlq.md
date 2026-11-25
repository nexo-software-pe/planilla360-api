# ADR 0007: Mensajería con reintentos y cola de mensajes muertos

- **Estado:** Aceptado. Decisión tomada en la Fase 5b; registrada en la Fase 10.
- **Fecha de la decisión (según el historial de git y la bitácora):** 2026-11-08 a 2026-11-09

## Contexto

Al cerrar una planilla hay que generar un PDF por trabajador (decenas o cientos) y enviarle un aviso por correo. Hacerlo dentro de la petición del cierre la volvería lenta y haría que el fallo de un PDF o del servidor de correo revirtiera o bloqueara el cierre. Además, cada trabajo puede fallar de forma independiente y transitoria (almacenamiento, SMTP).

## Decisión

Los trabajos pesados se procesan de forma asíncrona con **RabbitMQ**:

- **Un mensaje por trabajador.** `PlanillaCerradaListener` (módulo `boleta`) crea una boleta `PENDIENTE` por trabajador y encola un `TrabajoBoleta` en `boletas.generar`. Tras generar el PDF, el evento `BoletaGeneradaEvento` encola un `CorreoBoleta` en `notificaciones.correo`. Un fallo afecta a una boleta, no a la planilla.
- **Reintentos y DLQ por cola.** `MensajeriaConfig` (`compartido/mensajeria`) declara un exchange de trabajo y otro de mensajes muertos, y para cada cola una DLQ propia (`boletas.generar.dlq`, `notificaciones.correo.dlq`). El contenedor reintenta con espera creciente (`spring.rabbitmq.listener.simple.retry`: 3 reintentos, 1 s, factor 2, máximo 10 s, sin reencolar) y al agotarlos un `RepublishMessageRecoverer` publica el mensaje en la DLQ con el motivo en la cabecera `x-exception-message`.
- **Qué hace cada DLQ.** `ConsumidorBoletas` tiene un segundo listener sobre la DLQ que marca la boleta `FALLIDA`; `ConsumidorCorreos` registra el correo agotado. `POST /planillas/{id}/boletas/generar` relanza solo lo pendiente o fallido, y la generación es idempotente (`UNIQUE` planilla + empleado; no regenera si ya existe el PDF).
- **Contexto de empresa.** Los consumidores corren en hilos del listener, sin petición HTTP: cada uno fija `TenantContext` con la empresa del mensaje y lo limpia en `finally`.
- **Oyente posterior al commit.** `PlanillaCerradaListener` usa `@TransactionalEventListener(AFTER_COMMIT)`: el cierre ya está confirmado y un fallo aquí no lo revierte (se registra y RRHH puede relanzar). Como ese oyente sigue enlazado a la transacción terminada, todo lo que escriba en la base va con `REQUIRES_NEW` (`BoletaService`, `GeneracionBoletaService`).
- **Degradación.** `app.messaging.enabled` (variable `MESSAGING_ENABLED`, `false` por defecto) controla la topología (`MensajeriaConfig`) y los consumidores. Con `false` se activan `EnProcesoColaBoletas` y `EnProcesoColaCorreos`, que procesan en el mismo proceso: sin reintentos ni DLQ; un fallo deja la boleta en `FALLIDA` y se reintenta con el endpoint de generación.

## Consecuencias

- El cierre responde rápido y es independiente de PDF y correo; el progreso se ve en el estado de cada boleta.
- Los mensajes se serializan como JSON (`JacksonJsonMessageConverter`), no como objetos Java; los records de cada módulo son su contrato.
- Los mensajes de la DLQ requieren revisión humana o relanzamiento; las colas tienen métricas y panel (ver ADR 0008).
- Se puede ejecutar la API sin RabbitMQ (desarrollo, pruebas), a costa de perder reintentos automáticos.
- Los consumidores deben ser idempotentes, porque RabbitMQ garantiza entrega al menos una vez.
- El historial sembrado en local desactiva el listener (`CargaHistorica`) para no generar cientos de PDF.

## Alternativas consideradas

- **Procesar todo en la petición del cierre:** simple, pero lento y acopla el cierre al éxito de cada PDF y correo.
- **`@Async` o un pool de hilos propio:** sin persistencia ni reintentos; los trabajos se pierden si el proceso cae.
- **Un solo mensaje por planilla:** un fallo obligaría a reprocesar todos los trabajadores y no habría estado por boleta.
- **Reintentos con colas de espera (TTL):** más flexibles, pero más topología que mantener; el reintento del contenedor más la republicación a la DLQ cubre la necesidad.
- **Tabla de trabajos en la base de datos (outbox):** evitaría perder el mensaje entre el commit y el encolado; se descartó por ahora porque el relanzamiento manual cubre ese caso y el volumen es bajo.
