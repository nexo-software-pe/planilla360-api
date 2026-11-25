# ADR 0002: Monolito modular con Spring Modulith

- **Estado**: Aceptado. Decisión tomada en la Fase 0; registrada en la Fase 10
- **Fecha de la decisión (según el historial de git y la bitácora)**: 2026-11-07
- **Aplicación**: desde el esqueleto inicial de la API (Fase 1, con `ModulithStructureTests`).

## Contexto

Planilla360 cubre varios contextos con ritmos de cambio distintos: identidad, tenancy, personal, tiempo,
ausencias, remuneraciones, documentos, reportes y mensajería. El equipo es pequeño y el sistema se despliega
como una sola aplicación. Se necesitaba separar responsabilidades con fronteras claras sin pagar el costo
operativo de varios servicios (red, despliegues coordinados, transacciones distribuidas).

## Decisión

Un **monolito modular** con **Spring Modulith**. Cada módulo es un paquete raíz bajo
`pe.nexosoftware.planilla360` (`identidad`, `empresa`, `catalogo`, `empleado`, `asistencia`, `vacaciones`,
`planilla`, `boleta`, `importacion`, `reporte`, `auditoria`, `notificacion`, `integracion`, `compartido`).

Reglas de convivencia:

- Un módulo expone su **API pública** en el paquete raíz del módulo (por ejemplo `PlanillaApi`,
  `AsistenciaApi`, `EmpleadoApi`, `CatalogoApi`, `EmpresaApi`) junto con sus eventos. Los subpaquetes son
  internos.
- La comunicación entre módulos es por esa API pública o por **eventos** de aplicación.
- `compartido` es el kernel: módulo abierto (`ApplicationModule.Type.OPEN`) con value objects, contexto de
  tenant, seguridad y excepciones; el resto de módulos puede usar todos sus paquetes.
- La estructura se verifica en cada build: `ModulithStructureTests` ejecuta
  `ApplicationModules.of(Planilla360ApiApplication.class).verify()`, que falla ante dependencias cíclicas
  o accesos a internos de otro módulo. Las reglas de capas están en `ArquitecturaTests` (ArchUnit).

### Mapa de contextos: relaciones principales (no exhaustivo)

```
 identidad  ──(conforme)──────────► empresa            (tenant)
 empleado   ──(cliente/proveedor)─► catalogo           (áreas, cargos, AFP)
 asistencia ◄──(evento: VacacionesAprobadas, PermisoAprobado)── vacaciones
 planilla   ──consulta por puertos─► asistencia, vacaciones, empleado, catalogo, empresa
 planilla   ──evento: PlanillaCerradaEvento──► boleta
 boleta     ──evento: BoletaGeneradaEvento───► notificacion
 reporte    ──lectura SQL en solo lectura (vista y tablas)──► datos de varios módulos   (ADR 0005)
 auditoria  ◄──eventos de cambio en entidades sensibles
 integracion: clientes de servicios externos (DNI/RUC/tipo de cambio) con Resilience4j
 importacion: carga masiva por Excel de empleados
```

Otras relaciones reales, siempre por la API pública del otro módulo (comprobadas con las importaciones del
código):

- `asistencia` → `empleado` y `catalogo` (`EmpleadoModuloAdapter` implementa `ConsultaEmpleadoPort` y
  `ConsultaFeriadoPort`).
- `vacaciones` → `empleado` y `empresa` (`EmpleadoEmpresaModuloAdapter`).
- `boleta` → `planilla` (`PlanillaApi.datosParaBoletas`, además del evento), `empleado` y `empresa`.
- `notificacion` → `boleta` (`BoletaApi`, además del evento), `empleado` y `empresa`.
- `empleado` → `identidad` (`IdentidadApi`) y `catalogo`.
- `importacion` → `empleado` y `catalogo`.
- Los jobs de `asistencia`, `vacaciones` y `planilla` (en `infrastructure/scheduling`) → `empresa`
  (`EmpresaApi`) para recorrer las empresas activas.

| Módulo | Rol | Estructura interna |
|---|---|---|
| `asistencia`, `vacaciones`, `planilla` | Núcleo | Hexagonal ([ADR 0003](0003-hexagonal-solo-en-el-nucleo.md)) |
| `empleado`, `identidad`, `empresa`, `catalogo`, `boleta`, `importacion`, `reporte`, `auditoria`, `notificacion`, `integracion` | Soporte | Capas clásicas o adaptadores |
| `compartido` | Kernel | Value objects, tenant, seguridad |

`planilla` consulta a los demás módulos por **puertos de salida** (`ConsultaAsistenciaPort`,
`ConsultaVacacionesPort`, `ConsultaPersonalPort`, `ConsultaCatalogoPort`, `ConsultaEmpresaPort`) con
adaptadores en su capa `infrastructure` que llaman a la API pública del otro módulo; así el modelo de
planilla no se acopla a sus vecinos. La regla de puertos rige en `domain` y `application`; lo que vive en
`infrastructure` (por ejemplo los jobs de `scheduling`) puede usar la API pública de otro módulo
directamente.

## Consecuencias

- Un solo artefacto, una sola base de datos y transacciones locales: despliegue y depuración simples.
- Las fronteras no dependen de la disciplina: el build las comprueba. Romper una regla falla el CI.
- La comunicación por eventos desacopla flujos (cierre de planilla, boletas, correos) sin que los módulos
  se conozcan; el trabajo pesado o con reintentos va por mensajería aparte (ver [ADR 0007](0007-mensajeria-con-reintentos-y-dlq.md), mensajería con reintentos y DLQ).
- Si un módulo (por ejemplo `notificacion`) necesitara escalar solo, sus fronteras ya están definidas y se
  podría extraer; hoy no se justifica.
- Costo: todo comparte proceso y ciclo de despliegue, y los módulos comparten la base de datos físicamente
  (ver [ADR 0005](0005-reportes-sobre-base-compartida-solo-lectura.md) para el caso de lectura de reportes).

## Alternativas consideradas

- **Microservicios desde el inicio**: máxima independencia, pero añade red, consistencia eventual,
  observabilidad distribuida y despliegues coordinados que el tamaño del equipo y del producto no justifican.
- **Monolito en capas sin módulos**: más simple al principio, pero las dependencias cruzadas crecen sin
  control y no hay una frontera comprobable por contexto.
- **Módulos Maven separados**: aíslan en compilación, pero con más ceremonia de build; Modulith da la
  verificación de fronteras dentro de un solo módulo Maven.
