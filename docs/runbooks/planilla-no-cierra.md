# Runbook: la planilla no se puede cerrar (409 / 422)

## Cuándo usarlo

`POST /api/v1/planillas/{id}/cerrar` responde:

- **422** «Regla de negocio incumplida», con un detalle que empieza por «No se puede cerrar la planilla: ...» (validaciones pendientes) o con otra regla de la planilla (por ejemplo, la planilla ya está cerrada con otra clave o el estado no permite cerrar).
- **409** «Conflicto con el estado actual»: la `Idempotency-Key` ya se usó para cerrar otra planilla de la misma empresa.
- **400** si la petición **no trae** la cabecera `Idempotency-Key`: la rechaza Spring MVC antes de llegar al caso de uso («Required header 'Idempotency-Key' is not present»).
- **422** si la cabecera viene **vacía** (por ejemplo `-H "Idempotency-Key;"`) o supera los 100 caracteres: lo responde `CierrePlanillaService` («Indique la clave de idempotencia...», «no puede superar los 100 caracteres»).

## Contexto (fuente: `ValidadorCierre`, `ValidadorDePlanilla`, `CierrePlanillaService`, `Planilla`)

- Solo cierra una planilla en estado `CALCULADA` (BORRADOR → CALCULADA → CERRADA; ANULADA es salida definitiva). En `BORRADOR` primero hay que calcular.
- La cadena `ValidadorCierre.estandar()` reúne **todas** las incidencias, no solo la primera. Cada una tiene un código:

| Código | Causa | Aplica a |
|---|---|---|
| `SIN_REGIMEN_PENSIONARIO` | Un trabajador del periodo no tiene régimen pensionario (ONP o AFP) | todos los tipos |
| `ASISTENCIA_INCOMPLETA` | La asistencia del mes no está completa: el **último día del mes** aún no está consolidado, o algún día del mes está en estado `INCOMPLETA` (falta una marcación) | **solo planilla MENSUAL** |
| `VACACIONES_PENDIENTES` | Hay solicitudes de vacaciones por aprobar que se solapan con el periodo | **solo planilla MENSUAL** |
| `PLANILLA_DUPLICADA` | Ya existe otra planilla **cerrada** del mismo periodo y tipo | todos los tipos |
| `NETO_NEGATIVO` | Un trabajador tiene neto negativo en su línea | todos los tipos |

- Las validaciones de asistencia y vacaciones no condicionan la gratificación ni la CTS.
- La `Idempotency-Key` es **única por empresa** (índice `uq_planilla_clave` sobre `tenant_id` + clave). Repetir el cierre con la misma clave sobre la misma planilla ya cerrada devuelve la misma planilla con la cabecera `Idempotent-Replayed: true` y no cierra de nuevo.
- Crear una planilla ya está limitado: solo una planilla vigente (no anulada) por periodo y tipo.

## Diagnóstico

1. Consulta las incidencias (token de RRHH o ADMIN_EMPRESA):
   ```bash
   curl -s -H "Authorization: Bearer <accessToken>" \
     http://localhost:8080/api/v1/planillas/<planillaId>/validaciones
   ```
   Responde `{"valida": false, "incidencias": [{"codigo": "...", "mensaje": "..."}]}`.
2. Mira el estado de la planilla: `GET /api/v1/planillas/<planillaId>` (campo de estado).
3. Según el código:
   - `SIN_REGIMEN_PENSIONARIO`: el mensaje trae el id del trabajador; abre su ficha/contrato (`/api/v1/empleados`) y registra su régimen (ONP o AFP).
   - `ASISTENCIA_INCOMPLETA` (`NovedadesMensualesService.mesCompleto` exige las dos condiciones):
     - (a) El último día del mes debe estar consolidado. Hasta que lo esté (por el job de las 00:30, hora de Lima, o a mano, una vez terminado el día) la planilla mensual no cierra. A mano solo se puede consolidar un día ya terminado; si no, responde 422 «Solo se puede consolidar un día ya terminado».
     - (b) Ningún día puede estar `INCOMPLETA` (falta una marcación). Reconsolidar **no** lo arregla. **Hoy la API no ofrece un camino para corregir una marcación pasada**: `POST /api/v1/asistencia/marcaciones` no recibe cuerpo y registra la entrada o salida de quien llama en el instante actual (`RegistrarMarcacionUseCase.registrar`), y no existe un caso de uso en `asistencia/application/port/in` para corregir o ajustar marcaciones. La justificación de días (`JustificarDiasUseCase`) solo la dispara el evento de vacaciones o permisos aprobados, no tiene endpoint, y solo aplica a días sin marcaciones. Escala al **responsable técnico del api** y al **responsable de producto / soporte funcional**; no edites la base a mano.
     - Para ver los días `INCOMPLETA`: `GET /api/v1/asistencia?empleadoId=<uuid>&mes=AAAA-MM` (días de un trabajador) o `GET /api/v1/asistencia/resumen?mes=AAAA-MM` (resumen de todos). Ambos requieren RRHH o ADMIN_EMPRESA.
     - Para reconsolidar un día: `POST /api/v1/asistencia/consolidaciones` con `{"fecha":"AAAA-MM-DD"}` (RRHH o ADMIN_EMPRESA).
   - `VACACIONES_PENDIENTES`: aprueba o rechaza las solicitudes en `/api/v1/vacaciones` que se solapen con el periodo.
   - `PLANILLA_DUPLICADA` (poco frecuente: crear una planilla ya impide dos vigentes del mismo periodo y tipo, con 409 en `PlanillaService.crear`): busca la otra planilla cerrada con `GET /api/v1/planillas?periodo=<AAAA-MM>&tipo=<TIPO>&estado=CERRADA`. Si la cerrada es correcta, la que intentas cerrar sobra (anúlala); si la cerrada es errónea, hay que anularla antes (ver más abajo).
   - `NETO_NEGATIVO`: revisa en `GET /api/v1/planillas/<planillaId>/lineas` las líneas de ese trabajador (descuentos o adelantos mayores que los ingresos) y corrige el dato de origen.
4. Si responde 409 por la clave: ya se usó para cerrar otra planilla de la empresa. Esa comprobación ocurre **antes** de validar, así que sale haya o no incidencias (también puede salir por el índice `uq_planilla_clave` si dos cierres con la misma clave compiten a la vez). Genera una clave nueva (por ejemplo un UUID) y reintenta.

## Solución

1. Corrige la causa en el módulo de origen (empleados, asistencia, vacaciones).
2. Recalcula para que las líneas reflejen los datos corregidos:
   ```bash
   curl -s -X POST -H "Authorization: Bearer <accessToken>" \
     http://localhost:8080/api/v1/planillas/<planillaId>/calcular
   ```
3. Revisa otra vez `/validaciones` hasta que `valida` sea `true`.
4. Cierra con una clave de idempotencia **estable para esa operación**: si la petición se corta (timeout), repite con **la misma** clave; no generes otra.
   ```bash
   curl -s -X POST -H "Authorization: Bearer <accessToken>" \
     -H "Idempotency-Key: <clave-unica-por-empresa>" \
     http://localhost:8080/api/v1/planillas/<planillaId>/cerrar
   ```
5. **Anular** (solo `ADMIN_EMPRESA`; motivo obligatorio; solo si la planilla **no tiene pago registrado**, de lo contrario responde 409):
   ```bash
   curl -s -X POST -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
     -d '{"motivo":"<motivo de la anulación>"}' \
     http://localhost:8080/api/v1/planillas/<planillaId>/anular
   ```
   Si quien intenta anular es RRHH, la respuesta es 403: pide a un administrador de la empresa.

## Verificar

- `POST .../cerrar` responde `200` con la planilla en estado `CERRADA`.
- Repetir la misma petición con la misma clave responde `200` con `Idempotent-Replayed: true`.
- Tras el cierre, `GET .../boletas/estado` empieza a avanzar (las boletas se generan al confirmarse el cierre); si no avanza, usa [dlq-rabbitmq.md](dlq-rabbitmq.md).

## Escalar

- Dato de origen que no sabes corregir (régimen, contrato, asistencia): **responsable de producto / soporte funcional**.
- Planilla cerrada con error y pago ya registrado (no se puede anular): **responsable técnico del api** y **responsable de producto**; no se edita la base a mano.
- Incidencias que no corresponden a la realidad (validación equivocada): **responsable técnico del api**, con el `traceId` de la petición (ver [logs-por-traceid.md](logs-por-traceid.md)).
