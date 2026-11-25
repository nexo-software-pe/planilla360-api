# ADR 0005: Reportes sobre la base compartida, en solo lectura

- **Estado**: Aceptado
- **Fase**: decisión tomada en el diseño (Fase 0); implementada en la Fase 5b (módulo `reporte` y vista de la migración V8); registrada en la Fase 10.
- **Relación**: es la excepción acotada a la regla de fronteras del ADR 0002 y depende del filtro por tenant del ADR 0004.

## Contexto

Los reportes combinan datos de varios módulos (planilla, empleado, catálogo, asistencia, vacaciones) y
necesitan agregaciones que el modelo de objetos expresa mal: rankings con funciones de ventana, totales
por área y cargo con `ROLLUP`, saldos calculados. Resolverlos llamando a la API pública de cada módulo y
combinando en memoria implicaría traer muchos registros, repetir consultas por empresa y reimplementar en
Java agregaciones que SQL hace mejor.

## Decisión

El módulo `reporte` lee directamente de la base compartida con **SQL nativo de solo lectura** mediante
`JdbcClient` (`ReporteService`), sin entidades JPA propias ni escrituras.

- **Vista de lectura** `vista_planilla_detalle` (migración `V8__boletas_y_reportes.sql`): una fila por
  trabajador de cada planilla en estado `CERRADA`, con área, cargo y los importes de la línea. Es el
  contrato de datos entre el cálculo y los reportes de planilla.
- Los reportes de costo laboral (`GROUPING SETS`/`ROLLUP`) y de planilla consultan esa vista; el de
  tardanzas (CTE más `RANK() OVER (PARTITION BY ...)`) y el de saldos de vacaciones (CTE) consultan las
  tablas de asistencia y vacaciones con sus uniones a empleado y área.
- **Cada consulta filtra por `tenant_id`** (ADR 0004): la vista expone la columna y las consultas la piden
  como parámetro en cada tabla.
- `reporte` no modifica datos de otros módulos; solo lee.
- Es una decisión consciente de **Shared Database en solo lectura**, acotada a este módulo; los demás
  módulos no leen tablas ajenas.

## Consecuencias

- Reportes eficientes, con SQL expresivo (CTE, ventanas, `ROLLUP`) y la agregación hecha en la base.
- Acoplamiento al esquema físico: renombrar columnas de `planilla`, `linea_planilla`, `empleado`, `area` o
  `cargo` exige actualizar la vista y las consultas. La vista concentra ese acoplamiento para planilla, y
  `ReporteApiTests` ejecuta las consultas contra PostgreSQL real, por lo que un cambio incompatible falla
  en el build.
- Se rompe a propósito la regla de «hablar solo por API pública» para este caso de lectura; queda acotado
  a un módulo y documentado aquí. El resto del sistema mantiene las fronteras del ADR 0002.
- El aislamiento por empresa recae en el filtro explícito de cada consulta; se cubre con pruebas.
- Los reportes asumen PostgreSQL como base (CTE, funciones de ventana y `ROLLUP`).

## Alternativas consideradas

- **Llamar a la API pública de cada módulo**: respeta las fronteras, pero obliga a traer conjuntos grandes,
  combinar y agregar en memoria, con más consultas y menos expresividad; las funciones de ventana y los
  subtotales se reescribirían en Java.
- **Solo vistas de solo lectura** (todo reporte detrás de una vista): más desacoplado del esquema físico.
  Hoy solo la de planilla existe como vista y el resto de consultas va a tablas; pasar las demás a vistas
  es una evolución posible si el esquema empieza a cambiar con frecuencia.
- **Base de reportes separada (réplica o almacén analítico)**: aísla la carga de lectura, pero añade
  réplica, sincronización y operación que el volumen actual no justifica.
