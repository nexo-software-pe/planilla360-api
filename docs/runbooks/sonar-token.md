# Runbook: el job SonarCloud falla por autenticación

## Cuándo usarlo

El job `SonarCloud` del workflow `CI` termina en rojo con mensajes como `Not authorized`, `Invalid token` o `401`, o avisa de que falta `SONAR_TOKEN`. Si el job falla por el **quality gate** (cobertura, incidencias), este no es el runbook: abre el enlace del check en el PR y corrige el código.

## Contexto

- `SONAR_TOKEN` está registrado como secreto de organización de GitHub (organización `nexo-software-pe`) y lo usan los CI de `planilla360-api` y `planilla360-web`; es un dato **a confirmar por el administrador de la organización** (paso 5 del diagnóstico). Es un **token personal** de una cuenta de SonarCloud, creado con vencimiento «Never (if used)»: no caduca mientras se siga usando, pero deja de servir si se revoca o si su dueño pierde acceso a la organización.
- Organización en SonarCloud: `nexo-software-pe`. Proyectos: `nexo-software-pe_planilla360-api` y `nexo-software-pe_planilla360-web`.
- El **análisis automático está desactivado**: el análisis solo corre desde GitHub Actions (job `sonar` de `.github/workflows/ci.yml`, con `-Dsonar.qualitygate.wait=true`).
- Los PR de Dependabot **no ejecutan** el job: lleva `if: github.actor != 'dependabot[bot]'` porque Dependabot no recibe los secretos. Que el job aparezca como omitido (*skipped*) en un PR de Dependabot es lo esperado, no un fallo.

## Diagnóstico

1. Decide si el fallo es general o puntual: mira el job `SonarCloud` en el último CI de `staging` y en dos PR distintos.
   ```bash
   gh run list --workflow CI --branch staging --limit 5
   gh run view <id-de-la-corrida> --log-failed
   ```
2. Lee el error del paso «Análisis y quality gate»:
   - Mensaje de autenticación (`401`, `Not authorized`, token inválido): sigue este runbook.
   - El análisis terminó pero el gate está en `ERROR`: es de código, no de token.
3. Descarta el caso esperado: si el PR es de Dependabot, el job no corre y no hay nada que arreglar.
4. Descarta un PR desde un fork: los forks tampoco reciben secretos (el equipo trabaja con ramas del propio repositorio).
5. Confirma que el secreto existe y llega a ambos repositorios (solo el nombre, nunca el valor). Pide a un administrador de la organización que revise en GitHub: *Organization → Settings → Secrets and variables → Actions → `SONAR_TOKEN`* y su acceso a `planilla360-api` y `planilla360-web`.
6. Confirma en SonarCloud que los proyectos existen y que el dueño del token sigue siendo miembro de la organización con permiso de ejecutar análisis.

## Solución: renovar el token

Hacen falta dos roles: un miembro de la organización en SonarCloud con permiso de ejecutar análisis, y un administrador de la organización en GitHub.

1. En SonarCloud, con la cuenta que será dueña del token: *Account → Security → Generate Tokens*. Nombre sugerido: `planilla360-ci`. Vencimiento: `Never (if used)`.
2. Copia el valor **una sola vez** y pégalo directamente en el paso siguiente. No lo guardes en archivos, tickets ni chats.
3. En GitHub: *Organization `nexo-software-pe` → Settings → Secrets and variables → Actions → `SONAR_TOKEN` → Update secret*. Pega el valor y guarda. Comprueba que el acceso del secreto siga incluyendo ambos repositorios.
4. Revoca el token anterior en SonarCloud (*Account → Security*, botón *Revoke* del token viejo).
5. Si el token se **filtró** (apareció en un log, un chat o un commit), haz primero el paso 4 y avisa al responsable técnico del api: un valor expuesto no se «borra» editando el historial, se invalida revocándolo.

## Verificar

1. Vuelve a ejecutar el CI fallido:
   ```bash
   gh run rerun <id-de-la-corrida> --failed
   gh run watch <id-de-la-corrida>
   ```
2. El job `SonarCloud` debe terminar en verde y el proyecto en SonarCloud debe mostrar un análisis con la fecha de hoy.
3. Revisa también el job de SonarCloud de `planilla360-web`: si usa el mismo secreto debería quedar en verde; si no, confirma con el administrador de qué secreto lee ese repositorio.
4. Un PR de Dependabot sigue mostrando el job omitido; es correcto.

## Escalar

- Falta permiso para tocar secretos de la organización: **administrador de la organización en GitHub**.
- El proyecto no aparece en SonarCloud o el análisis automático se reactivó: **responsable técnico del api**.
- Sospecha de filtración del token: **responsable técnico del api** de inmediato, después de revocar.
