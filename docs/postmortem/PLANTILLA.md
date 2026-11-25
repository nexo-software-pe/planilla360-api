# Postmortem: <título breve del incidente>

> Postmortem **sin culpa**: busca entender cómo el sistema y el proceso permitieron el incidente, no quién se equivocó. Describe decisiones y condiciones, nunca juzgues a personas. Copia este archivo a `docs/postmortem/AAAA-MM-DD-<slug>.md` y rellénalo dentro de los 5 días hábiles siguientes a resolver el incidente.

| Campo | Valor |
|---|---|
| Fecha del incidente | AAAA-MM-DD |
| Severidad | baja / media / alta |
| Estado | borrador / revisado / cerrado |
| Redactado por (rol) | |

Los ejemplos en cursiva de cada sección son **ficticios** y muestran el nivel de detalle esperado; bórralos al rellenar.

## 1. Resumen

Dos o tres frases: qué pasó, cuánto duró y cómo se resolvió.

*Ejemplo: el 2030-03-04 el cierre de la planilla mensual de febrero de una empresa cliente respondió 422 durante 3 horas porque quedaban solicitudes de vacaciones por aprobar. Se aprobaron las solicitudes y el cierre se completó a las 11:40.*

## 2. Impacto

Quién se vio afectado, cuánto y de qué forma (usuarios, empresas, planillas, boletas, dinero, plazos). Usa cifras.

*Ejemplo: 1 empresa, 42 trabajadores. Las boletas se entregaron 3 horas más tarde de lo previsto. Sin pérdida de datos ni pagos incorrectos.*

## 3. Línea de tiempo

Horas en hora de Lima, en orden. Una fila por hecho observable (detección, decisiones, acciones, resolución).

| Hora | Hecho |
|---|---|
| 08:35 | RRHH intenta cerrar la planilla y recibe 422. |
| 08:50 | Soporte abre el ticket con el `traceId` de la petición. |
| 09:20 | La guardia consulta `/validaciones`: 5 incidencias `VACACIONES_PENDIENTES`. |
| 11:15 | El jefe de área aprueba las solicitudes. |
| 11:40 | Cierre exitoso; las boletas empiezan a generarse. |

## 4. Causa raíz

Qué condición hizo posible el incidente y por qué existía. Aplica los «cinco porqués» hasta llegar a algo que se pueda cambiar en el sistema o el proceso, no a una persona.

*Ejemplo: el cierre exige que no haya vacaciones por aprobar. No existía un aviso previo al fin de mes, así que las solicitudes pendientes se descubrieron al intentar cerrar.*

## 5. Detección

Cómo nos enteramos (alerta, usuario, revisión) y cuánto tardamos. ¿Debió detectarse antes? ¿Qué señal habría ayudado?

*Ejemplo: lo reportó el usuario 15 minutos después del primer intento. No había alerta; el panel de Grafana mostraba el 422 pero nadie lo miraba.*

## 6. Qué salió bien

Lo que funcionó y conviene conservar.

*Ejemplo: la respuesta traía el detalle de la incidencia; el `traceId` permitió localizar la petición en segundos; el reintento con la misma `Idempotency-Key` no duplicó el cierre.*

## 7. Qué salió mal

Lo que falló, fue lento o dependió de la suerte (sin nombres propios).

*Ejemplo: la guía de cierre no mencionaba las vacaciones pendientes; hubo dudas sobre quién debía aprobar.*

## 8. Acciones correctivas

Cada acción con un responsable (rol o nombre del equipo) y fecha límite. Prefiere acciones que eviten la repetición (automatizar, avisar, documentar) antes que «tener más cuidado».

| Acción | Tipo (prevenir / detectar / mitigar) | Responsable | Fecha |
|---|---|---|---|
| Mostrar en la pantalla de planillas las vacaciones por aprobar 3 días antes del cierre | prevenir | responsable de producto | 2030-03-18 |
| Alerta cuando el cierre responde 422 más de una vez en una hora | detectar | responsable técnico del api | 2030-03-25 |
| Añadir el caso al runbook `planilla-no-cierra.md` | mitigar | responsable de guardia | 2030-03-11 |

## 9. Lecciones

Lo aprendido que otras personas del equipo deberían saber. Si es una regla general, propón añadirla a `arquitectura/LECCIONES.md` (síntoma → causa → regla).

*Ejemplo: las validaciones previas al cierre dependen de datos de otros equipos; un aviso anticipado cuesta menos que una guardia.*
