# Runbook: triaje de PR de Dependabot

## Cuándo usarlo

Cada lunes (06:00, hora de Lima) Dependabot abre PR hacia `staging` con actualizaciones de dependencias. Úsalo para decidir qué se integra, qué se revisa y qué se cierra, y cuando un PR de Dependabot queda en rojo.

## Contexto (fuente: `.github/dependabot.yml`)

| Ecosistema | Prefijo del commit | Grupos |
|---|---|---|
| Maven (`pom.xml`) | `chore(deps)` | `spring` (`org.springframework*`, `org.springdoc*`), `pruebas` (Testcontainers, WireMock, Mockito, AssertJ, Cucumber, ArchUnit, Awaitility) y `resto` (todo lo demás); los tres solo con actualizaciones `minor` y `patch` |
| Docker | `chore(deps)` | sin grupos |
| GitHub Actions | `ci(deps)` | `acciones` (todas en un solo PR) |

- Todos apuntan a `staging`. Maven tiene un límite de 5 PR abiertos.
- Los saltos **mayores** no entran en los grupos: llegan como PR individuales. Los mayores de `org.springframework.boot:*` se ignoran a propósito (los decide el equipo).
- Dependabot no recibe los secretos de la organización, así que el job `SonarCloud` se **omite** en sus PR (ver [sonar-token.md](sonar-token.md)). Los demás checks del CI sí corren y son los que deciden.

## Diagnóstico y decisión

1. Lista los PR abiertos:
   ```bash
   gh pr list --author "app/dependabot" --base staging
   ```
2. Mira el estado de los checks de cada uno:
   ```bash
   gh pr checks <número>
   ```
3. Clasifica según el salto de versión (el título dice «bump X from A to B»):

| Caso | Acción |
|---|---|
| Patch o minor con CI verde (`SonarCloud` omitido es normal) | Lee las notas de versión de lo que sube; si nada afecta al proyecto, merge a `staging` como cualquier otro PR. |
| Grupo (`spring`, `pruebas`, `resto`, `acciones`) en verde | Revisa en la descripción qué paquetes suben y haz merge. |
| Mayor | No se integra por rutina: pasa a revisión del responsable técnico (cambios incompatibles, migraciones, licencias). |
| CI en rojo | `gh run view <id> --log-failed`. Si el arreglo está en nuestro código, corrígelo en la misma rama del PR; si la actualización no es viable, cierra el PR con un comentario del motivo. |
| Aviso de seguridad (CVE) | Prioridad alta: integra la versión corregida y corre el CI completo. |

4. Si el PR queda «not up to date» con `staging` (la protección exige la rama al día), comenta `@dependabot rebase` y espera el CI otra vez antes del merge.

## Mitigación

- Un cambio que rompe algo detectado **después** del merge: revierte con un PR de `revert` hacia `staging`; no se reescribe el historial.
- Para dejar de recibir una dependencia o un salto mayor: comenta `@dependabot ignore this dependency` (o `ignore this major version`) en el PR, o ajusta `.github/dependabot.yml` con un PR normal.

## Verificar

- Tras el merge, mira el CI del **push a `staging`** (el análisis de la rama puede ver cosas que el del PR no vio):
  ```bash
  gh run list --workflow CI --branch staging --limit 3
  ```
- `staging` debe quedar verde; si no, revertir el PR es la primera acción.
- La bandeja de PR de Dependabot queda vacía o solo con los mayores en revisión.

## Escalar

- Mayores, CVE o dependencias que no compilan: **responsable técnico del api**.
- Dependabot no abre PR o su ejecución falla: **administrador de la organización en GitHub** (el repositorio muestra el error en *Insights → Dependency graph → Dependabot*).
