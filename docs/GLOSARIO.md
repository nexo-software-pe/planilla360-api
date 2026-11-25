# Glosario del dominio (lenguaje ubicuo)

Este documento reúne los términos del negocio de planillas peruanas tal como los usa el código de Planilla360. Sirve para que el equipo (y quien se incorpora) hable igual en las conversaciones, en el código y en la API.

**Importante:** todos los valores legales (RMV, UIT, porcentajes, comisiones de AFP, feriados) son **parámetros configurables** que viven en la tabla `parametro_legal` y en los catálogos (carga inicial en `src/main/resources/db/migration/V2__datos_referenciales.sql`). Las cifras son **ficticias y de aprendizaje**: no son asesoría legal ni tributaria. El dominio nunca las lleva escritas: las recibe como `ParametrosLegales`.

Convenciones del código: el dominio está en español (`Planilla`, `Gratificacion`), la infraestructura en inglés, y todo monto es `Dinero` (nunca `double`; dos decimales, redondeo mitad hacia arriba). En «Código» las rutas son relativas a `pe.nexosoftware.planilla360`. En «En la API» se indica cómo se nombra el concepto en los datos que expone `/api/v1` (JSON en español, fechas ISO-8601, periodos `yyyy-MM`).

## Índice

[AFP](#afp) · [Anulación](#anulación) · [Asignación familiar](#asignación-familiar) · [Bonificación extraordinaria](#bonificación-extraordinaria) · [Boleta](#boleta) · [Boleta generada, enviada y fallida](#boleta-generada-enviada-y-fallida) · [Bounded context (módulo)](#bounded-context-módulo) · [Cierre](#cierre) · [CTS](#cts) · [DLQ](#dlq-cola-de-mensajes-muertos) · [Empresa cliente (tenant)](#empresa-cliente-tenant) · [Gratificación](#gratificación) · [Hora extra](#hora-extra) · [Idempotency-Key](#idempotency-key) · [Marcación](#marcación) · [ONP](#onp) · [Pago de planilla](#pago-de-planilla) · [Periodo](#periodo) · [Permiso](#permiso) · [Planilla](#planilla) · [Récord vacacional (periodo vacacional)](#récord-vacacional-periodo-vacacional) · [Régimen laboral](#régimen-laboral) · [Régimen pensionario](#régimen-pensionario) · [Remuneración asegurable](#remuneración-asegurable) · [Remuneración computable](#remuneración-computable) · [Retención de 5.ª categoría](#retención-de-5ª-categoría) · [RMV](#rmv) · [Solicitud de vacaciones](#solicitud-de-vacaciones) · [Tardanza](#tardanza) · [Tolerancia](#tolerancia) · [UIT](#uit)

---

## AFP
Sistema privado de pensiones. El descuento del mes se compone de tres partes que la boleta muestra por separado: **aporte obligatorio al fondo** (`AFP_APORTE_PCT`, 10 % de referencia), **prima de seguro** (`AFP_PRIMA_PCT`, 1,37 %) y **comisión** de la administradora (distinta por AFP y por tipo de comisión: `FLUJO` o `MIXTA`; cada AFP del catálogo trae ambas). Cada componente se calcula sobre la [remuneración asegurable](#remuneración-asegurable) y se redondea por separado.
- **Código:** `planilla/domain/regimen/Afp`, `DescuentoPension` (aporte, prima, comisión), `TipoComision`; catálogo en `catalogo` (tabla `afp`).
- **En la API:** `afpId` y `tipoComision` en el contrato del empleado; `descuentoPension` en la línea de planilla es el **total**; el desglose está en los conceptos de la boleta (`AFP_APORTE`, `AFP_PRIMA`, `AFP_COMISION`).
- **Nota:** hay dos `Afp`, dos `TipoComision` y dos `TipoRegimenPensionario` con el mismo nombre en módulos distintos.
  - `Afp`: `catalogo/entity/Afp` (persistida) frente a `planilla/domain/regimen/Afp` (cálculo). `CatalogoModuloAdapter` lee la AFP del catálogo (`AfpResumen`) y la traduce a `AfpVigente` (nombre y comisiones de flujo y mixta); `FabricaRegimenPensionario` construye el `Afp` del dominio con la comisión que corresponde al tipo. Al leer una línea ya guardada, `PlanillaPersistenceAdapter` (`aDominio`) construye también el `Afp` del dominio, directamente desde el nombre y el porcentaje de comisión guardados en la línea, sin pasar por la fábrica.
  - `TipoComision`: `empleado/model/TipoComision` frente a `planilla/domain/regimen/TipoComision`; lo traduce `PersonalModuloAdapter`.
  - `TipoRegimenPensionario`: `empleado/model/TipoRegimenPensionario` frente a `planilla/domain/regimen/TipoRegimenPensionario`; lo traduce `PersonalModuloAdapter` (con `valueOf` sobre el texto del régimen que expone el contrato del empleado).

## Anulación
Salida definitiva de una planilla: pasa a `ANULADA` y no vuelve atrás. Solo la puede hacer el **administrador de la empresa**, con un **motivo obligatorio**, y solo si la planilla **no tiene pago registrado**. Se permite desde `BORRADOR`, `CALCULADA` y `CERRADA`. El cambio queda en la auditoría con el motivo y el usuario.
- **Código:** `planilla/application/service/AnulacionPlanillaService`; la transición está en `EstadoPlanilla.alAnular()`.
- **En la API:** `POST /api/v1/planillas/{id}/anular` con el motivo; estado `ANULADA`.

## Asignación familiar
Monto fijo mensual por trabajador igual al **10 % de la [RMV](#rmv)** (`ASIG_FAMILIAR_PCT`), que se paga si tiene **al menos un hijo elegible**. Es elegible el hijo **menor de 18 años** o, si **estudia**, hasta los **24 años** (deja de serlo al cumplir 25). La edad se evalúa al último día del mes del periodo. Se paga una sola vez aunque haya varios hijos elegibles. Forma parte de la remuneración computable y de la asegurable.
- **Código:** `planilla/domain/service/CalculadoraAsignacionFamiliar`, `planilla/domain/model/Hijo` (nacimiento y si estudia).
- **En la API:** `asignacionFamiliar` en la línea de planilla; los hijos son familiares del empleado (`fechaNacimiento`, `estudia`).

## Bonificación extraordinaria
Porcentaje adicional sobre la [gratificación](#gratificación) (9 %, parámetro `BONIF_EXTRAORDINARIA_PCT`). Se calcula sobre el monto de la gratificación **ya ajustado por el [régimen laboral](#régimen-laboral)**.
- **Código:** `planilla/domain/service/CalculadoraGratificacion` (resultado `Gratificacion`: monto y bonificación), `ParametrosLegales.pctBonificacionExtraordinaria`.
- **En la API:** `bonificacionExtraordinaria` en la línea de una planilla de tipo `GRATIFICACION`.

## Boleta
Comprobante de pago de un trabajador para una planilla cerrada: desglosa ingresos, descuentos, aportes del empleador y neto. Se genera como PDF y se guarda en el almacén de archivos.
- **Código:** módulo `boleta` (`entity/Boleta`, `service/GeneracionBoletaService`, `pdf/OpenPdfGeneradorBoleta`); los conceptos salen de `planilla/domain/model/Concepto` y `LineaPlanilla`.
- **En la API:** `GET /api/v1/planillas/{planillaId}/boletas`, `GET /api/v1/boletas/mias` (el trabajador ve las suyas) y `GET /api/v1/boletas/{id}/pdf`.

## Boleta generada, enviada y fallida
Ciclo de vida de una boleta: `PENDIENTE` → `GENERADA` (el PDF existe) → `ENVIADA` (se envió por correo) o `FALLIDA` (la generación falló; el motivo queda registrado). Cuándo se llega a `FALLIDA` depende de `app.messaging.enabled`: con `true` (valor por defecto del perfil `local`) los trabajos pasan por RabbitMQ, se reintentan con espera creciente y solo al agotarlos van a la [DLQ](#dlq-cola-de-mensajes-muertos) y la boleta queda `FALLIDA`; con `false` (valor por defecto general, `MESSAGING_ENABLED=false`) se genera en el mismo proceso, **sin reintentos automáticos**, y el primer fallo deja la boleta `FALLIDA`. En ambos modos se reintenta a mano con `POST /api/v1/planillas/{id}/boletas/generar`. Al cerrar una planilla se encola un trabajo de generación por trabajador; el PDF solo se puede descargar en `GENERADA` o `ENVIADA`.
- **Código:** `boleta/model/EstadoBoleta`, `boleta/mensajeria/ConsumidorBoletas`, módulo `notificacion` (correo).
- **En la API:** `estado` de la boleta; `POST /api/v1/planillas/{planillaId}/boletas/generar` para pedir la generación y `GET .../boletas/estado` para ver el avance.

## Bounded context (módulo)
Parte del sistema con su propio vocabulario y sus propias reglas. Planilla360 es un **monolito modular**: cada módulo (`empresa`, `identidad`, `empleado`, `catalogo`, `asistencia`, `vacaciones`, `planilla`, `boleta`, `notificacion`, `reporte`, `importacion`, `integracion`, `auditoria`) se comunica con los demás a través de su interfaz pública (`*Api`) o de eventos. `compartido` es el único módulo abierto (tenant, seguridad, excepciones, `Dinero`, `Periodo`, `Ruc`, `Dni`). Una palabra puede significar cosas distintas en cada contexto: «periodo» en `vacaciones` es un año de servicio; en `planilla` es un mes.
- **Código:** un paquete por módulo bajo `pe.nexosoftware.planilla360`. En `planilla`, el dominio (`domain`) no depende de Spring ni de JPA.

## Cierre
Acción que vuelve **definitiva** una planilla `CALCULADA`: pasa a `CERRADA`, guarda la [clave de idempotencia](#idempotency-key) y la fecha de cierre (quién la cerró consta en la auditoría), y publica el evento `PlanillaCerrada` (que dispara la generación de [boletas](#boleta)). Antes de cerrar se ejecuta una cadena de validaciones: asistencia del mes consolidada y sin solicitudes de vacaciones por aprobar que se solapen con el periodo (**estas dos solo en la planilla mensual**, no en gratificación ni CTS), todos los trabajadores con [régimen pensionario](#régimen-pensionario), ningún neto negativo y ninguna otra planilla cerrada del mismo periodo y tipo. Si falla alguna, la planilla no se cierra y se informan las incidencias. Cerrar de nuevo una planilla ya cerrada con la misma clave no tiene efecto; con **otra** clave es un error.
- **Código:** `planilla/application/service/CierrePlanillaService`, `planilla/domain/validation/*`, `planilla/domain/event/PlanillaCerrada`.
- **En la API:** `POST /api/v1/planillas/{id}/cerrar` (cabecera `Idempotency-Key`); `GET /api/v1/planillas/{id}/validaciones` para ver las incidencias sin cerrar.

## CTS
Compensación por tiempo de servicio. Se deposita dos veces al año: en **mayo** (semestre noviembre-abril) y en **noviembre** (semestre mayo-octubre). La remuneración computable es básico + asignación familiar + promedio de horas extra + **1/6 de la última gratificación**. El monto es `computable / 12` por **mes completo** más `computable / 12 / 30` por **día completo** (mes comercial de 30 días; meses entre 0 y 6 y días entre 0 y 29). Al armar la planilla solo entra el personal **vigente el último día del semestre** (quien cesó antes no tiene línea) y quien no llega a **un día completo** en el semestre tampoco tiene línea (la gratificación exige un mes completo; la CTS, un día). Se aplica el porcentaje del [régimen laboral](#régimen-laboral): 100 % en el general, 50 % en pequeña empresa.
- **Código:** `planilla/domain/service/CalculadoraCts` (resultado `DepositoCts`), `CalculadoraTiempoDeServicio` (meses y días completos según ingreso y cese), `SemestreDeBeneficio.deCts`.
- **En la API:** planilla de tipo `CTS` (`POST /api/v1/planillas/cts`).

## DLQ (cola de mensajes muertos)
Cola donde termina un mensaje cuyo procesamiento falló tras agotar los reintentos. Cada cola de trabajo (`boletas.generar`, `notificaciones.correo`) tiene su DLQ (`*.dlq`). En el negocio importa porque una boleta cuyo trabajo llega a la DLQ se marca `FALLIDA` con el motivo (cabecera `x-exception-message`). Solo existe con la mensajería habilitada (`app.messaging.enabled=true`).
- **Código:** `compartido/mensajeria/Colas` y `MensajeriaConfig`; `boleta/mensajeria/ConsumidorBoletas.marcarFallida`.

## Empresa cliente (tenant)
Empresa que contrata Planilla360; cada una ve **solo sus datos**. Se identifica por su RUC y tiene un [régimen laboral](#régimen-laboral). El aislamiento es por columna `tenant_id` en toda tabla de negocio; el tenant sale del JWT y un recurso de otra empresa se responde 404. Roles: `ADMIN_SAAS` (opera todas las empresas), `ADMIN_EMPRESA`, `RRHH` y `EMPLEADO`.
- **Código:** módulo `empresa`; `compartido/tenant/TenantContext`, `compartido/infrastructure/TenantContextFilter`, `compartido/seguridad/Rol`.
- **En la API:** `/api/v1/empresas` (solo `ADMIN_SAAS`); `empresaId` en los mensajes de trabajo asíncronos.

## Gratificación
Pago extra de **julio** (semestre enero-junio) y **diciembre** (semestre julio-diciembre). Fórmula: `remuneración computable × meses completos / 6`, con meses completos entre 0 y 6 (mes comercial de 30 días; el tiempo se cuenta desde el ingreso, o desde el inicio del semestre, hasta el fin del semestre). Al armar la planilla solo entra el personal **vigente el último día del semestre** (30 de junio o 31 de diciembre): quien cesó antes no tiene línea. Además, quien no llega a **un mes completo** en el semestre tampoco tiene línea. Luego se aplica el porcentaje del [régimen laboral](#régimen-laboral) (100 % o 50 %) y se agrega la [bonificación extraordinaria](#bonificación-extraordinaria). No entra en la remuneración asegurable del mes ni en la pensión.
- **Código:** `planilla/domain/service/CalculadoraGratificacion`, `CalculadoraTiempoDeServicio`, `planilla/domain/model/SemestreDeBeneficio.deGratificacion` (falla si el periodo no es julio ni diciembre).
- **En la API:** planilla de tipo `GRATIFICACION` (`POST /api/v1/planillas/gratificacion`); `bonificacionExtraordinaria` en la línea.

## Hora extra
Tiempo trabajado fuera del horario que se **aprueba** (`PENDIENTE` → `APROBADA` o `RECHAZADA`); solo las aprobadas se pagan. Valor hora = `sueldo básico / 30 / horas de la jornada diaria`; las **dos primeras horas de cada día** llevan la sobretasa 1 (25 %) y las siguientes la sobretasa 2 (35 %). Cada día se redondea una sola vez. Para la gratificación y la CTS el promedio semestral de horas extra solo cuenta si hubo horas extra en **3 meses o más** del semestre.
- **Código:** `planilla/domain/service/CalculadoraHorasExtra`, `asistencia/domain/model/HoraExtra` y `EstadoHoraExtra`; parámetros `HE_SOBRETASA_1_PCT` y `HE_SOBRETASA_2_PCT`.
- **En la API:** `/api/v1/asistencia/horas-extra`; `horasExtra` en la línea de planilla.

## Idempotency-Key
Cabecera HTTP obligatoria al **cerrar** una planilla. Es una clave elegida por el cliente (hasta 100 caracteres) que identifica la intención de cerrar. Repetir la petición con la misma clave devuelve la misma planilla cerrada, con la cabecera de respuesta `Idempotent-Replayed: true`, sin cerrar dos veces ni publicar dos eventos. Usar la misma clave para cerrar **otra** planilla es un conflicto. La fila de la planilla se bloquea durante la transacción, así que dos peticiones simultáneas se serializan. Sirve para reintentar sin riesgo ante un corte de red.
- **Código:** `planilla/application/service/CierrePlanillaService`; columna `idempotency_key` de la tabla `planilla`, con índice único **por empresa** y parcial (`(tenant_id, idempotency_key)` donde la clave no es nula, `V6__planilla.sql`): dos empresas pueden usar la misma clave.
- **En la API:** cabecera de petición `Idempotency-Key`; cabecera de respuesta `Idempotent-Replayed`.

## Marcación
Registro de entrada o salida de un trabajador. El tipo **alterna** (`ENTRADA`, `SALIDA`) según la última del día. Se rechazan: una marcación con **menos de 60 segundos** de diferencia con la anterior, una anterior a la última registrada y más de **8 por día**. Cada noche se **consolida** el día en una asistencia diaria con los minutos de [tardanza](#tardanza), los minutos extra y un estado (`PRESENTE`, `TARDANZA`, `FALTA`, `JUSTIFICADA`, `FERIADO`, `DESCANSO`, `INCOMPLETA`). El día se evalúa en hora de Lima y los minutos se truncan (08:10:59 cuenta como 08:10).
- **Código:** `asistencia/domain/service/PoliticaMarcacion`, `ConsolidadorAsistencia`, `asistencia/domain/model/Marcacion`.
- **En la API:** `POST /api/v1/asistencia/marcaciones`; `POST /api/v1/asistencia/consolidaciones` para consolidar a pedido.

## ONP
Sistema Nacional de Pensiones: un único aporte porcentual (`ONP_PCT`, 13 % de referencia) sobre la [remuneración asegurable](#remuneración-asegurable). Sin prima ni comisión.
- **Código:** `planilla/domain/regimen/Onp`.
- **En la API:** no se expone como campo propio: `regimenPensionario` = `ONP` en el empleado y `descuentoPension` en la línea.

## Pago de planilla
Registro de que una planilla **cerrada** ya se pagó: fecha de pago y **referencia** obligatoria (por ejemplo el número de operación). Solo se registra sobre una planilla `CERRADA` y una sola vez (un segundo registro es un conflicto). Una planilla con pago ya no se puede [anular](#anulación).
- **Código:** `planilla/application/service/PagoPlanillaService`, `planilla/application/model/PagoPlanilla`.
- **En la API:** `POST /api/v1/planillas/{id}/pago`.

## Periodo
Mes calendario de una planilla, con formato `yyyy-MM` (por ejemplo `2026-07`). Es un valor inmutable y ordenable que sabe su primer y último día y cuántos días tiene. El mes determina qué tipo de planilla tiene sentido: gratificación en 7 y 12, CTS en 5 y 11.
- **Código:** `compartido/dominio/Periodo`.
- **En la API:** campo `periodo` como texto `yyyy-MM`.

## Permiso
Ausencia puntual del trabajador (`MEDICO`, `PERSONAL` o `LICENCIA`), con o sin goce de haber. Se solicita (`SOLICITADO`) y RRHH lo aprueba o rechaza. Al aprobarse, sus días quedan **justificados** en asistencia y no cuentan como falta en la asistencia. Pero el permiso **sin goce** sí descuenta en la planilla mensual: sus días se suman a las inasistencias y se descuentan a `básico / 30` por día (ver [Tardanza](#tardanza)).
- **Código:** `vacaciones/domain/model/Permiso`, `TipoPermiso`, `EstadoPermiso`; el evento `PermisoAprobado` lo recoge `asistencia`.
- **En la API:** `/api/v1/permisos` (`.../aprobar`, `.../rechazar`).

## Planilla
Cálculo de lo que se paga a todos los trabajadores de una empresa en un [periodo](#periodo). Tiene un **tipo** (`MENSUAL`, `GRATIFICACION`, `CTS`) y un **estado**: `BORRADOR` → `CALCULADA` → `CERRADA`, y `ANULADA` como salida definitiva. Mientras está en borrador o calculada se puede recalcular; cerrada ya no se modifican sus líneas. Contiene una **línea** por trabajador (básico proporcional a los días laborados, asignación familiar, horas extra, bonos, descuento de asistencia, pensión, retención de 5.ª categoría, otros descuentos, aporte de EsSalud del empleador y neto). El neto es ingresos menos descuentos.
- **Código:** `planilla/domain/model/Planilla`, `LineaPlanilla`, `EstadoPlanilla`, `TipoPlanilla`; cálculo mensual en `CalculadoraLineaMensual`.
- **En la API:** `/api/v1/planillas` (`/calcular`, `/lineas`, `/validaciones`, `/cerrar`, `/anular`, `/pago`).

## Récord vacacional (periodo vacacional)
Año de servicio del trabajador y los días de vacaciones que generó. Se **gana un periodo al cumplir cada año** desde la fecha de ingreso (30 días en régimen general y 15 en pequeña empresa). El **saldo** es `ganados − gozados − vendidos` y nunca puede quedar negativo. Simplificación del curso: no se exige el mínimo legal de días efectivamente trabajados; un empleado cesado no gana periodos después del cese.
- **Código:** `vacaciones/domain/model/PeriodoVacacional`, `PoliticaVacaciones`, `vacaciones/domain/service/CalculadoraPeriodos`.
- **En la API:** `diasGanados`, `diasGozados`, `diasVendidos`; `POST /api/v1/vacaciones/periodos/{id}/venta` para vender días.

## Régimen laboral
Régimen de la **empresa** que define los beneficios: **`GENERAL`** (gratificación y CTS al 100 %, 30 días de vacaciones al año, se pueden vender hasta 15) y **`PEQUENA_EMPRESA`** (gratificación y CTS al 50 %, 15 días de vacaciones, no se pueden vender).
- **Código:** `planilla/domain/regimen/RegimenLaboral` (Strategy); en vacaciones, `PoliticaVacaciones` (sellada: `General`, `PequenaEmpresa`).
- **En la API:** `regimenLaboral` de la empresa.

## Régimen pensionario
Sistema de pensiones elegido por **cada trabajador**: [ONP](#onp) o [AFP](#afp). Define el descuento mensual por pensión. Un trabajador sin régimen pensionario impide el [cierre](#cierre) de la planilla.
- **Código:** `planilla/domain/regimen/RegimenPensionario` (interfaz sellada: `Onp`, `Afp`), `TipoRegimenPensionario`, `FabricaRegimenPensionario`.
- **En la API:** `regimenPensionario` (`ONP` o `AFP`), `afpId` y `tipoComision` en el contrato del empleado.

## Remuneración asegurable
Base sobre la que se calculan la **pensión** y el aporte de **EsSalud** (`ESSALUD_PCT`, 9 %, a cargo del empleador): **básico del mes + asignación familiar + horas extra + bonos**. No incluye gratificación, bonificación extraordinaria ni CTS.
- **Código:** `planilla/domain/model/LineaPlanilla.remuneracionAsegurable()`; cálculo de pensión en `planilla/domain/regimen`.
- **En la API:** se deriva de los campos de la línea (`basico`, `asignacionFamiliar`, `horasExtra`, `bonos`).

## Remuneración computable
Base de la [gratificación](#gratificación) y la [CTS](#cts). En la gratificación es **básico + asignación familiar + promedio semestral de horas extra** (la suma de los 6 meses entre 6, solo si hubo horas extra en 3 meses o más). En la CTS se suma además **1/6 de la última gratificación**.
- **Código:** `CalculadoraGratificacion.remuneracionComputable` y `CalculadoraCts.calcular`.
- **En la API:** no se expone como campo propio; solo se ve el resultado (`gratificacion`, `cts`) en la línea.

## Retención de 5.ª categoría
Adelanto mensual del impuesto a la renta del trabajo dependiente. Versión simplificada del curso: se **proyecta la renta anual** (remuneración fija = básico + asignación familiar, sin horas extra, × meses restantes del año, incluido el actual, + gratificaciones que aún se pagarán + ingresos ya percibidos), se **deducen 7 [UIT](#uit)** y se aplica una escala acumulativa por tramos de renta neta (hasta 5 UIT: 8 %; hasta 20: 14 %; hasta 35: 17 %; hasta 45: 20 %; más: 30 %). La retención del mes es el impuesto anual entre 12. Si la renta bruta anual no supera 7 UIT, no hay retención. Las gratificaciones pendientes de proyectar son dos hasta julio y una después; cada una se proyecta como de semestre completo, con su bonificación extraordinaria y ajustada por el régimen laboral.
- **Código:** `planilla/domain/service/CalculadoraRenta5ta`, usada por `CalculadoraLineaMensual`.
- **En la API:** `retencion5ta` en la línea de planilla.

## RMV
Remuneración mínima vital (parámetro `RMV`, valor ficticio de aprendizaje). Se usa para la [asignación familiar](#asignación-familiar) y debe ser mayor que cero.
- **Código:** `ParametrosLegales.rmv`.
- **En la API:** no se expone como campo propio; es el parámetro legal `RMV` del catálogo.

## Solicitud de vacaciones
Pedido del trabajador para gozar días de su [récord vacacional](#récord-vacacional-periodo-vacacional). Recorre una máquina de estados: `SOLICITADA` → `APROBADA_JEFE` → `APROBADA_RRHH` → `EN_GOCE` → `FINALIZADA`; se puede rechazar (`RECHAZADA`) o cancelar (`CANCELADA`) mientras no tenga la aprobación de RRHH. Mientras está activa **reserva sus días**, de modo que no se pueden pedir más de los que hay en el saldo. Una solicitud por aprobar que se solape con el periodo bloquea el [cierre](#cierre).
- **Código:** `vacaciones/domain/model/SolicitudVacaciones`, `EstadoSolicitud`.
- **En la API:** `/api/v1/vacaciones/solicitudes` (`/aprobar-jefe`, `/aprobar-rrhh`, `/rechazar`, `/cancelar`).

## Tardanza
Llegada posterior a la hora de entrada **que supera la [tolerancia](#tolerancia)**. Si llega dentro de la tolerancia no hay tardanza; si la supera, se cuentan **todos los minutos desde la hora de entrada**, no solo el exceso. Se descuenta al valor minuto: `básico × minutos / (30 × horas de jornada × 60)`. Las inasistencias descuentan `básico / 30` por día: son las faltas injustificadas más los días de [permiso](#permiso) sin goce. El descuento total nunca supera el **básico del mes** (el proporcional a los días laborados, no el del contrato).
- **Código:** `asistencia/domain/service/ConsolidadorAsistencia`, `planilla/domain/service/CalculadoraDescuentoAsistencia`.
- **En la API:** minutos de tardanza en la asistencia diaria; `descuentoAsistencia` en la línea de planilla.

## Tolerancia
Minutos de gracia después de la hora de entrada del horario (0 a 60, y siempre menor que la jornada). La hora límite sin tardanza es `entrada + tolerancia`.
- **Código:** `asistencia/domain/model/Horario` (`toleranciaMin`, `limiteSinTardanza()`).
- **En la API:** `toleranciaMin` del horario (`/api/v1/horarios`).

## UIT
Unidad impositiva tributaria (parámetro `UIT`, valor ficticio de aprendizaje). Es la unidad en la que se expresan la deducción y los tramos de la [retención de 5.ª categoría](#retención-de-5ª-categoría).
- **Código:** `ParametrosLegales.uit`.
- **En la API:** no se expone como campo propio; es el parámetro legal `UIT` del catálogo.
