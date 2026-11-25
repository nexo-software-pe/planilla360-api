# Registro de decisiones de arquitectura (ADR)

Cada ADR documenta una decisión relevante: contexto, decisión, consecuencias y alternativas consideradas. Todos están aceptados. Cada uno indica en su cabecera la fase en que se tomó la decisión, la fecha según el historial de git y la bitácora, y la Fase 10 como la de su registro.

| ADR | Título | En una línea |
|---|---|---|
| [0001](#adr-0001-reservado) | Reservado | Número reservado; no hay documento. |
| [0002](0002-monolito-modular-con-spring-modulith.md) | Monolito modular con Spring Modulith | Una sola aplicación dividida en módulos con fronteras verificadas por pruebas. |
| [0003](0003-hexagonal-solo-en-el-nucleo.md) | Arquitectura hexagonal solo en el núcleo | `asistencia`, `vacaciones` y `planilla` usan puertos y adaptadores; el resto, capas clásicas. |
| [0004](0004-multi-tenant-por-columna.md) | Multi-tenant por columna (`tenant_id`) | Todas las empresas comparten tablas y cada fila lleva su `tenant_id`, que Hibernate filtra solo en las entidades de negocio; `usuario` y `auditoria_cambio` (tenant anulable) y el SQL nativo filtran de forma explícita. |
| [0005](0005-reportes-sobre-base-compartida-solo-lectura.md) | Reportes sobre la base compartida, en solo lectura | El módulo `reporte` lee la base compartida con SQL nativo de solo lectura (una vista para planilla y costo laboral, tablas para tardanzas y vacaciones), siempre filtrando por `tenant_id`. |
| [0006](0006-cierre-de-planilla-idempotente.md) | Cierre de planilla idempotente | El cierre exige una clave de idempotencia y repetirlo devuelve la misma planilla sin duplicar efectos. |
| [0007](0007-mensajeria-con-reintentos-y-dlq.md) | Mensajería con reintentos y cola de mensajes muertos | Boletas y correos se procesan de forma asíncrona con RabbitMQ, reintentos y una DLQ por cola. |
| [0008](0008-observabilidad-traceid-propio-y-micrometer.md) | Observabilidad con traceId propio y Micrometer | Un `traceId` por petición en logs y cabecera, y métricas Prometheus con panel en Grafana. |
| [0009](0009-contrato-openapi-versionado.md) | Contrato OpenAPI versionado | El contrato vive en el repositorio y el CI falla ante los cambios de nivel `ERR` que rompen a los clientes de `/api/v1`; lo incompatible va en `/api/v2`. |

## ADR 0001: reservado

El número 0001 está reservado y no tiene documento en este repositorio. Los ADR del api empiezan en el 0002 para no chocar con él.

## Cómo añadir un ADR

1. Toma el número siguiente al último de la tabla y crea `NNNN-titulo-en-minusculas.md`.
2. Usa las secciones Contexto, Decisión, Consecuencias y Alternativas consideradas, y una cabecera con Estado, fecha de la decisión y fase.
3. Para una decisión ya implementada, indica la fase en que se tomó (según `git log` del código que la aplica) y en cuál se registró; si otro ADR trata el tema, cítalo con un enlace relativo y di si lo amplía.
4. Añade la fila a la tabla de este índice.
