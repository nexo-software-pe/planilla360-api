# ADR 0006: Cierre de planilla idempotente

- **Estado:** Aceptado. Decisión tomada en la Fase 5a; registrada en la Fase 10.
- **Fecha de la decisión (según el historial de git y la bitácora):** 2026-11-08

## Contexto

Cerrar una planilla es la operación más sensible del sistema: congela los montos, habilita el pago y dispara la generación de boletas y correos para cada trabajador. Un cliente que reintenta por un corte de red, un doble clic o dos pestañas abiertas no debe producir un doble cierre, ni un segundo evento `PlanillaCerrada` (que generaría boletas y correos duplicados).

## Decisión

El cierre es idempotente y se garantiza en tres capas:

1. **Clave obligatoria.** `POST /api/v1/planillas/{id}/cerrar` exige la cabecera `Idempotency-Key` (máximo 100 caracteres). Sin ella, `CierrePlanillaService` rechaza la petición con una regla de negocio.
2. **Agregado.** `Planilla.cerrar(clave, validación, instante)` devuelve el evento `PlanillaCerrada` solo la primera vez. Si la planilla ya está cerrada con la misma clave devuelve `Optional.empty()` (no hay nada que hacer); si está cerrada con otra clave lanza una excepción de dominio.
3. **Aplicación y base de datos.**
   - La fila se lee con bloqueo `PESSIMISTIC_WRITE` (`PlanillaJpaRepository`): dos peticiones simultáneas con la misma clave se serializan y la segunda ve la planilla ya cerrada.
   - Si la clave ya identifica a otra planilla de la empresa, se responde 409 (`ConflictoException`).
   - La migración `V6__planilla.sql` crea el índice único parcial `uq_planilla_clave (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL`, de modo que la clave es única **por empresa**, y `uq_planilla_cerrada (tenant_id, periodo, tipo) WHERE estado = 'CERRADA'`, que impide dos planillas cerradas del mismo periodo y tipo. Un `CHECK` exige clave y fecha de cierre en toda planilla cerrada.

Repetir la petición con la misma clave devuelve la misma planilla cerrada, con la cabecera `Idempotent-Replayed: true`, y **no** vuelve a publicar `PlanillaCerrada` ni a registrar el cambio en la auditoría. Al repetirse tampoco se vuelven a ejecutar las validaciones de cierre.

## Consecuencias

- Los clientes pueden reintentar con seguridad; el front debe generar una clave por intento de cierre y reutilizarla en los reintentos.
- Las boletas y los correos se disparan una sola vez por cierre.
- El bloqueo de fila serializa solo las peticiones sobre la misma planilla; no afecta a otras empresas ni a otras planillas.
- La clave tiene alcance de empresa: dos empresas pueden usar la misma clave sin colisión.
- Reutilizar una clave en otra planilla es un error del cliente (409), no un reintento.

## Alternativas consideradas

- **Sin clave, basándose solo en el estado `CERRADA`:** evita el doble cierre, pero no distingue un reintento legítimo de un segundo cierre intencional ni permite devolver "la misma respuesta".
- **Bloqueo optimista (`@Version`):** una de las peticiones simultáneas fallaría con un error de concurrencia que el cliente tendría que interpretar; con el bloqueo pesimista la segunda petición obtiene el resultado correcto.
- **Almacén de claves en Redis:** añade una dependencia para una garantía que la base de datos ya da de forma transaccional y que debe sobrevivir a la ausencia de Redis (la API degrada sin él).
- **Tabla aparte de claves de idempotencia:** más general, pero innecesaria mientras solo el cierre sea idempotente; la clave vive en la propia planilla.
