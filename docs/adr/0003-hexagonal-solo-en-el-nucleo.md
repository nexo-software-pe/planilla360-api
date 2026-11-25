# ADR 0003: Arquitectura hexagonal solo en el núcleo

- **Estado**: Aceptado. Decisión tomada en la Fase 0; registrada en la Fase 10
- **Fecha de la decisión (según el historial de git y la bitácora)**: 2026-11-07
- **Aplicación**: en `planilla` (dominio en la Fase 3; aplicación e infraestructura en la Fase 5a) y en `asistencia` y `vacaciones` (Fase 4).
- **Relación**: concreta, para la estructura interna de los módulos, lo decidido en el [ADR 0002](0002-monolito-modular-con-spring-modulith.md).

## Contexto

Las reglas con más valor y más riesgo del producto están en tres módulos: `asistencia` (tardanzas, horas
extra), `vacaciones` (saldos, aprobaciones) y `planilla` (cálculo, gratificación, CTS, renta de 5ta
categoría, cierre). Son reglas de negocio peruanas que cambian con la normativa y que deben probarse sin
levantar Spring ni una base de datos. Los demás módulos son sobre todo mantenimiento de datos (CRUD),
integración o infraestructura, donde esa separación aporta poco.

## Decisión

- **Hexagonal (puertos y adaptadores)** solo en `asistencia`, `vacaciones` y `planilla`, con tres capas:
  - `domain`: Java puro (modelo, servicios de dominio, regímenes, eventos de dominio).
  - `application`: casos de uso (`port.in`) y puertos de salida (`port.out`) con sus servicios.
  - `infrastructure`: persistencia JPA, web, adaptadores a otros módulos, planificación y eventos.
- **Capas clásicas** (`controller / service / repository / dto / entity`) en los módulos de soporte.
- Se hace cumplir con ArchUnit en cada build:
  - `ArquitecturaTests` (todo el código): las clases de cualquier paquete `domain` no dependen de Spring,
    JPA ni Hibernate; los controllers no usan repositorios ni entidades; los servicios no conocen los
    controllers; los DTO no dependen de entidades ni repositorios; las entidades JPA viven en paquetes
    `entity`; inyección solo por constructor.
  - `ArquitecturaHexagonalTests` (`asistencia` y `vacaciones`): el dominio solo depende de
    `asistencia.domain`, `vacaciones.domain`, `compartido.dominio` y `java..` (la regla no impide que el
    dominio de uno use el del otro; hoy no ocurre; no de `compartido.tenant`, `seguridad` ni
    `excepcion`) y no conoce `application` ni `infrastructure`; la aplicación no depende de
    `infrastructure`, ni de JPA, Hibernate, Spring Data, Spring Web o HTTP, ni accede a `empleado`,
    `empresa`, `catalogo` ni `identidad`; `infrastructure.web` no depende de `infrastructure.persistence`;
    las clases de `infrastructure.persistence.adapter` implementan una interfaz de `port.out`.
  - `ArquitecturaPlanillaTests` (`planilla`): el dominio solo depende de `planilla.domain`,
    `compartido.dominio` y `java..`, y no conoce `application` ni `infrastructure`; el modelo y los
    regímenes no dependen de las calculadoras (`domain.service`), que no guardan estado mutable; la
    aplicación no depende de `infrastructure`, de JPA, Hibernate, Spring Data, Spring Web ni HTTP, ni de
    `empleado`, `empresa`, `catalogo`, `identidad`, `asistencia` ni `vacaciones`; `infrastructure.web` no
    depende de `infrastructure.persistence`; las clases de nivel superior de
    `infrastructure.persistence.adapter`, `infrastructure.adapter` e `infrastructure.event` implementan
    una interfaz de `port.out`.
- Cuando el núcleo necesita datos de otro módulo, lo pide por un puerto de salida y un adaptador de
  `infrastructure` que llama a la API pública de ese módulo (capa anticorrupción ligera).

## Consecuencias

- Las calculadoras y reglas se prueban con pruebas unitarias rápidas, sin contexto de Spring.
- Cambiar cómo se guarda o se expone algo no toca las reglas.
- Más clases y algo de mapeo (entidad de persistencia a modelo de dominio) en los tres módulos núcleo;
  se acepta porque ahí está el valor del negocio.
- Hay dos estilos en el código. La regla es simple: núcleo hexagonal, soporte en capas. Los tests
  de arquitectura protegen las fronteras del núcleo; nada obliga a que un módulo de soporte siga en capas,
  es una convención del proyecto.
- Un módulo de soporte que gane complejidad de dominio puede migrar a hexagonal más adelante, con el mismo
  criterio y los mismos tests.

## Alternativas consideradas

- **Capas clásicas en todo**: menos código y un solo estilo, pero el dominio quedaría acoplado a JPA y
  Spring, y las reglas de planilla se probarían con más infraestructura de por medio.
- **Hexagonal en todos los módulos**: coherencia total, pero en módulos de CRUD (áreas, cargos, usuarios)
  duplicaría clases sin aportar reglas que proteger.
