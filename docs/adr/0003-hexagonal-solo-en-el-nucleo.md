# ADR 0003: Arquitectura hexagonal solo en el núcleo

- **Estado**: Aceptado
- **Fecha**: 2026-11-07 (diseño aprobado, Fase 0)

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
  - `ArquitecturaTests`: el dominio no depende de Spring, JPA ni Hibernate; los controllers no usan
    repositorios ni entidades; los servicios no conocen los controllers; los DTO no dependen de entidades;
    las entidades JPA viven en paquetes `entity`; inyección solo por constructor.
  - `ArquitecturaHexagonalTests` (`asistencia` y `vacaciones`) y `ArquitecturaPlanillaTests` (`planilla`):
    el dominio solo usa Java y el kernel `compartido`; el dominio no conoce `application` ni
    `infrastructure`; la aplicación no usa JPA, Spring Data ni la capa web, y no accede directamente a otros
    módulos; la capa web no toca la persistencia; los adaptadores de persistencia implementan un puerto de
    salida.
- Cuando el núcleo necesita datos de otro módulo, lo pide por un puerto de salida y un adaptador de
  `infrastructure` que llama a la API pública de ese módulo (capa anticorrupción ligera).

## Consecuencias

- Las calculadoras y reglas se prueban con pruebas unitarias rápidas, sin contexto de Spring.
- Cambiar cómo se guarda o se expone algo no toca las reglas.
- Más clases y algo de mapeo (entidad de persistencia a modelo de dominio) en los tres módulos núcleo;
  se acepta porque ahí está el valor del negocio.
- Hay dos estilos en el código. La regla es simple: núcleo hexagonal, soporte en capas; los tests de
  arquitectura avisan si un módulo se sale de su estilo.
- Un módulo de soporte que gane complejidad de dominio puede migrar a hexagonal más adelante, con el mismo
  criterio y los mismos tests.

## Alternativas consideradas

- **Capas clásicas en todo**: menos código y un solo estilo, pero el dominio quedaría acoplado a JPA y
  Spring, y las reglas de planilla se probarían con más infraestructura de por medio.
- **Hexagonal en todos los módulos**: coherencia total, pero en módulos de CRUD (áreas, cargos, usuarios)
  duplicaría clases sin aportar reglas que proteger.
