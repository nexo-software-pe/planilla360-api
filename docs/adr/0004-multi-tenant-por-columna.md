# ADR 0004: Multi-tenant por columna (`tenant_id`)

- **Estado**: Aceptado
- **Fase**: decisión tomada en el diseño (Fase 0); implementada en la Fase 2 (contexto de tenant, filtro y pruebas de aislamiento); registrada en la Fase 10.

## Contexto

Planilla360 es un SaaS: varias empresas cliente (tenants) comparten la misma aplicación y los datos de
una no deben ser visibles ni modificables por otra. Se esperan muchas empresas de tamaño pequeño o medio,
con migraciones de esquema frecuentes durante el desarrollo y un equipo reducido para operar.

## Decisión

Una sola base de datos y un solo esquema, con la columna **`tenant_id`** en las tablas de negocio.

- **Contexto de tenant**: `TenantContext` guarda el tenant de la petición en un `ThreadLocal`.
  `TenantContextFilter` lo puebla con la empresa del JWT ya validado (si hay usuario autenticado) y lo
  limpia en un `finally`, de modo que un hilo del servidor no arrastre la empresa de otra petición.
  También deja la empresa en el MDC de los logs como `tenantId`.
- **Filtro automático**: las entidades de negocio (empleados, contratos, familiares, áreas, cargos,
  marcaciones, horarios, solicitudes de vacaciones, planillas y sus líneas, boletas, etc.) marcan su
  columna con `@TenantId` de Hibernate. `HibernateTenantConfig` registra un resolvedor que toma el tenant
  de `TenantContext`; Hibernate añade el filtro en las lecturas y asigna el valor al insertar.
- **Fallo seguro**: si no hay empresa en contexto, el resolvedor devuelve un UUID nulo (`0000...`) que no
  coincide con ninguna fila: ante la duda, no se devuelve nada.
- **Excepciones explícitas**: `usuario` y `auditoria_cambio` tienen `tenant_id` anulable (el administrador
  de la plataforma no pertenece a una empresa), por lo que sus entidades no usan `@TenantId`; los
  servicios (por ejemplo `UsuarioService`) validan el alcance de forma explícita según el rol del actor.
- **SQL nativo**: lo que no pasa por Hibernate (reportes con `JdbcClient`) **siempre filtra por
  `tenant_id`** con un parámetro (`:tenant`) en las tablas consultadas y en la vista de lectura.
- **Pruebas**: `AislamientoMultiTenantTests`, `TenantFilterTests` y `ReporteApiTests` verifican que una
  empresa no lista, lee, edita ni borra datos de otra y que un registro ajeno se trata como inexistente.

## Consecuencias

- Una sola migración Flyway y un solo pool de conexiones para todas las empresas; dar de alta una empresa
  no requiere infraestructura nueva.
- El aislamiento depende de la aplicación, no de la base de datos: un olvido en una consulta nativa
  expondría datos de otra empresa. Por eso el SQL nativo tiene una regla explícita, está concentrado en el
  módulo `reporte` y se cubre con pruebas de aislamiento. Quien añada una consulta nativa debe incluir el
  filtro y su prueba.
- Las restricciones únicas de negocio incluyen el tenant (por ejemplo `uq_area_nombre` sobre
  `(tenant_id, nombre)`: el mismo nombre de área puede existir en empresas distintas).
- Los índices de consultas por empresa llevan `tenant_id` como primera columna (por ejemplo
  `ix_cargo_tenant_area`).
- No hay aislamiento físico ni respaldos por cliente; si un cliente lo exigiera por contrato, habría que
  reevaluar esta decisión.

## Alternativas consideradas

- **Esquema por tenant**: mejor aislamiento lógico y respaldos por cliente, pero cada migración debe
  aplicarse a N esquemas, el catálogo de objetos crece con los clientes y hace falta enrutar la conexión
  por petición.
- **Base de datos por tenant**: el mayor aislamiento, pero el costo de operación (aprovisionamiento,
  migraciones, conexiones, respaldos, monitoreo) crece con el número de empresas y no se justifica para
  clientes pequeños.
- **Filtrado solo manual en cada consulta** (sin `@TenantId`): más simple en apariencia, pero depende de que
  cada desarrollador recuerde el filtro; se prefirió que el filtro sea el comportamiento por defecto.
