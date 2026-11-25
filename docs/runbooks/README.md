# Runbooks operativos

Guías cortas para resolver los incidentes más probables de la API. Cada runbook sigue el mismo orden: **cuándo usarlo** (síntoma), **diagnóstico**, **mitigación o solución**, **cómo verificar** y **a quién escalar**.

| Runbook | Úsalo cuando... |
|---|---|
| [sonar-token.md](sonar-token.md) | El job `SonarCloud` del CI falla por autenticación (token ausente, vencido o revocado). |
| [dependabot.md](dependabot.md) | Llegan PR de Dependabot y hay que decidir cuáles se integran y cuáles se revisan. |
| [dlq-rabbitmq.md](dlq-rabbitmq.md) | Las boletas no se generan o los correos de aviso no llegan. |
| [planilla-no-cierra.md](planilla-no-cierra.md) | `POST /api/v1/planillas/{id}/cerrar` responde 409 o 422. |
| [logs-por-traceid.md](logs-por-traceid.md) | Alguien reporta un error y hay que reconstruir qué pasó en esa petición. |

La plantilla para documentar un incidente una vez resuelto está en [`../postmortem/PLANTILLA.md`](../postmortem/PLANTILLA.md).

## Roles de escalamiento

Los runbooks nombran roles, no personas. Quién ocupa cada rol lo define el equipo en su rotación vigente.

| Rol | Responsabilidad |
|---|---|
| Responsable de guardia | Primer contacto; ejecuta el runbook. |
| Responsable técnico del api | Decisiones sobre código, datos y reversiones. |
| Administrador de la organización en GitHub | Secretos de organización, permisos y protección de ramas. |
| Responsable de producto / soporte funcional | Impacto en clientes y reglas de negocio (planillas, boletas). |

## Convenciones de estos documentos

- Los comandos están escritos para Git Bash o una terminal POSIX; lo que va entre `<...>` lo reemplazas tú.
- Ningún runbook contiene valores de secretos. Los tokens y contraseñas se leen de donde se guardan; nunca se pegan en tickets, chats ni en el repositorio.
- Los puertos y servicios locales salen de `compose.yaml` (perfiles `messaging`, `storage` y `obs`). Todos están publicados solo en `127.0.0.1`.
