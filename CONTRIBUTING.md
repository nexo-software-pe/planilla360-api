# Guía de contribución

Gracias por contribuir a **planilla360-api**. Esta guía explica cómo trabajamos, qué revisamos y cuándo un cambio está terminado. Para levantar el proyecto por primera vez, empieza por el [README](README.md).

## Índice
1. [Flujo de trabajo](#flujo-de-trabajo)
2. [Ramas](#ramas)
3. [Commits](#commits)
4. [Versionado](#versionado)
5. [Entorno y comandos](#entorno-y-comandos)
6. [Calidad: Checkstyle, Spotless, JaCoCo, PIT y SonarCloud](#calidad)
7. [Contrato OpenAPI](#contrato-openapi)
8. [Convenciones de código](#convenciones-de-código)
9. [Pruebas y TDD](#pruebas-y-tdd)
10. [Seguridad](#seguridad)
11. [Revisión de PR](#revisión-de-pr)
12. [Dependabot](#dependabot)
13. [Definition of Done](#definition-of-done)

## Flujo de trabajo
1. **Issue**: toma un issue (o crea uno) y muévelo a *In progress* en el tablero.
2. **Rama**: créala desde `staging` actualizado (`git switch staging && git pull && git switch -c fix/23-calculo-cts`).
3. **Commits** pequeños y convencionales (ver más abajo).
4. **PR a `staging`**: usa la plantilla completa (`.github/pull_request_template.md`), enlaza el issue (`Cierra #23`) y espera la revisión y el CI en verde.
5. **Release**: un PR `staging` → `main` publica lo acumulado en `staging`. Solo se abre cuando `staging` está verde y estable.
6. **Hotfix**: un incidente urgente se corrige en `hotfix/<n>-<slug>` creada desde `main`, con PR a `main`. Después, la corrección vuelve a `staging` con un PR `main` → `staging` (está protegida), para que no se pierda.

`main` y `staging` están protegidas: no se hace push directo, todo entra por PR y deben estar verdes los checks **Build y tests**, **Escaneo de secretos** y **Contrato OpenAPI**. Nunca uses `git push --force` en ramas compartidas.

## Ramas
Formato `<tipo>/<n>-<slug>`, donde `<n>` es el número del issue y el slug va en minúsculas, con guiones y en español:

| Tipo | Para qué | Ejemplo |
|---|---|---|
| `feat/` | funcionalidad nueva | `feat/41-export-excel` |
| `fix/` | corrección de un defecto | `fix/23-calculo-cts` |
| `chore/` | mantenimiento, dependencias, configuración | `chore/57-actualiza-testcontainers` |
| `docs/` | solo documentación | `docs/60-guia-onboarding` |
| `test/` | solo pruebas | `test/62-casos-limite-gratificacion` |
| `ci/` | flujos de CI | `ci/65-cachea-maven` |
| `hotfix/` | urgencia desde `main` | `hotfix/70-token-expirado` |

## Commits
Usamos [Conventional Commits](https://www.conventionalcommits.org/es/): `tipo(ámbito opcional): descripción en imperativo, en español`. Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `ci`. Ejemplos reales:

```
feat(planilla): calcula la gratificación de julio y diciembre
fix(asistencia): descuenta la tardanza solo después de la tolerancia
test(vacaciones): reproduce el saldo negativo al vender días
refactor(planilla): extrae la jornada diaria a JornadaLaboral
docs: documenta el flujo de cierre de planilla en el README
chore(deps): actualiza springdoc a la última versión de parche
ci: reintenta la descarga de gitleaks
```

Una sola idea por commit. Los cambios que rompen compatibilidad llevan `!` (`feat(api)!: ...`) y un pie `BREAKING CHANGE:`; en `/api/v1` no se permiten (ver [Contrato OpenAPI](#contrato-openapi)).

## Versionado
Seguimos [versionado semántico](https://semver.org/lang/es/) (`MAYOR.MENOR.PARCHE`): `feat` sube el menor, `fix` el parche y un cambio incompatible, el mayor. Más adelante, cada release a `main` llevará su entrada en un `CHANGELOG.md` generado a partir de los commits convencionales; es un proceso futuro y todavía no existe.

## Entorno y comandos
- **JDK 21** (LTS). El build falla con otra versión (maven-enforcer). Si tu `java` por defecto es otro, apunta `JAVA_HOME` al 21 (ver README).
- **Docker Desktop** encendido: los tests de integración usan Testcontainers. Compruébalo con `docker info`.
- En PowerShell usa `.\mvnw.cmd` en lugar de `./mvnw`.

```bash
./mvnw verify                                  # todo: compila, tests, Checkstyle, Spotless, JaCoCo y PIT
./mvnw test -Dtest='ValidadorCierreTests*'     # un test o clase (el `*` final es opcional: sirve para ejecutar y contar cada clase `@Nested` por separado)
./mvnw test -Dtest=ContratoOpenApiTests        # solo el contrato OpenAPI
./mvnw -q validate                             # solo Checkstyle y Spotless (rápido)
./mvnw spotless:apply                          # formatea el código (ver más abajo)
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage  # solo PIT (compila antes; ya corre dentro de verify)
```

`./mvnw verify` debe pasar en tu máquina **antes** de abrir el PR; es el mismo comando que ejecuta el CI.

## Calidad
Todo lo siguiente corre dentro de `./mvnw verify` y también en el CI.

- **Checkstyle** (`config/checkstyle/checkstyle.xml`): reglas de estilo sobre `src/main` y `src/test`; un aviso rompe el build. Se ejecuta en la fase `validate` (`./mvnw -q validate`). Lee el mensaje: indica archivo y línea.
- **Spotless**: comprueba imports sin usar, espacios al final de línea y salto de línea final. Si falla, corrígelo solo con `./mvnw spotless:apply` y vuelve a hacer commit.
- **JaCoCo**: cobertura mínima **80 %** (líneas y ramas) en los dominios y la aplicación de planilla, asistencia y vacaciones, y en `compartido/dominio`. Informe: `target/site/jacoco/index.html`.
- **PIT** (mutation testing): puntaje mínimo **85 %** sobre las clases de dominio de `planilla`, `asistencia`, `vacaciones` y `compartido.dominio`. Informe: `target/pit-reports/index.html`; abre las mutaciones «survived» para ver qué aserción falta. PIT solo ejecuta los tests de los paquetes `domain` de planilla, asistencia y vacaciones y de `compartido.dominio`, así que los tests del dominio van ahí. Ya corre dentro de `verify` (ejecución `pitest-dominio`, fase `verify`). Un cambio no se da por bueno si baja alguno de los dos umbrales.
- **SonarCloud**: análisis estático y de cobertura que corre en el CI (job `SonarCloud`) y evalúa el **quality gate**: un conjunto de condiciones (sin bugs ni vulnerabilidades nuevos, cobertura y duplicación del código nuevo dentro de lo exigido, puntos críticos de seguridad revisados). En un PR solo se mide el código que cambia. Para ver el detalle, abre el check `SonarCloud` en la pestaña *Checks* del PR y sigue el enlace al análisis (o el comentario de SonarCloud), donde cada incidencia indica archivo, línea y cómo corregirla. Si falla el gate, corrige las incidencias en el mismo PR. Los PR de Dependabot no se analizan. Tras un merge a `staging`, revisa también el CI del push: el análisis de la rama ve archivos que el del PR no ve.
- **Otros checks del CI**: *Escaneo de secretos* (Gitleaks), *Imagen Docker y Trivy* (vulnerabilidades en la imagen) y CodeQL.

## Contrato OpenAPI
El contrato público está en `docs/openapi/openapi.json` y lo valida `ContratoOpenApiTests`.

- **Cuándo regenerarlo**: siempre que cambies endpoints, parámetros, cabeceras o DTO. Hazlo en el mismo PR:
  ```bash
  ./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true
  ```
  Revisa el diff del JSON: debe contener solo lo que querías cambiar.
- **Compatibilidad**: el job `Contrato OpenAPI` compara tu contrato con el de la rama destino usando **oasdiff** (niveles en `.github/oasdiff-niveles.txt`). Si rompes `/api/v1` (quitar o renombrar un campo, endurecer una validación, cambiar un tipo) el PR falla. Lo incompatible va en `/api/v2`; lo compatible (campos opcionales nuevos, endpoints nuevos) sigue en `/api/v1`.
- Más detalle en [docs/contrato-api.md](docs/contrato-api.md).

## Convenciones de código
- **Idioma**: el dominio en **español** (clases, métodos de negocio, eventos, tablas, endpoints); la infraestructura en **inglés** (configuración, filtros, adaptadores técnicos y sufijos `Controller`, `Service`, `Repository`).
- Dinero siempre con `Dinero`, nunca `double`. Fechas con `LocalDate`/`Periodo`; el dominio no llama a `LocalDate.now()`, recibe la fecha como dato.
- Parámetros legales (RMV, UIT, tasas) se leen del catálogo, nunca se escriben en el código.
- Controllers reciben y devuelven DTO (records), nunca entidades.
- Cada módulo solo usa la API pública de otro; `ArquitecturaTests` lo hace cumplir.
- El mapa de «dónde vive qué» y el resto de reglas están en [CLAUDE.md](CLAUDE.md); si tu cambio altera ese mapa (módulo, paquete o endpoint nuevo), actualízalo.

## Pruebas y TDD
- Cada cambio de comportamiento lleva sus tests.
- **TDD en el dominio** (planilla, asistencia, vacaciones): tres pasos, preferiblemente en commits separados.
  1. `test:` en **rojo**: el test nuevo falla (con un esqueleto que compila y lanza `UnsupportedOperationException`).
  2. `feat:` o `fix:` en **verde**: la implementación mínima que lo pasa.
  3. `refactor:` si hace falta mejorar el diseño sin cambiar el comportamiento, con los tests en verde.
- **Ante un bug, primero el test que lo reproduce**: escríbelo, comprueba que falla por la razón correcta y solo entonces arregla. Así el defecto no vuelve.
- Los tests de integración heredan de `IntegracionBase` (Testcontainers). Las pruebas usan fechas de 2030 para que los JWT sigan siendo válidos.

## Seguridad
- **Nunca** subas secretos, contraseñas, tokens ni datos reales de personas (ni en código, ni en tests, ni en commits, ni en el PR). Usa variables de entorno; `.env.example` documenta las disponibles. Gitleaks bloquea el PR si detecta uno; si se te escapa uno, avisa de inmediato para rotarlo (borrarlo del historial no basta).
- **Multi-tenant**: cada empresa ve solo sus datos. Las entidades de negocio se filtran solas con `@TenantId`, pero **todo SQL nativo (reportes, `JdbcClient`) debe filtrar `tenant_id` a mano**. Un recurso de otra empresa se responde 404.
- Nunca incluyas contraseñas en las instantáneas de auditoría ni en los logs.

## Revisión de PR
El reviewer comprueba:
- que el PR cumple el issue y es pequeño y enfocado;
- que hay tests que fallarían sin el cambio (y, ante un bug, que reproducen el defecto);
- reglas de negocio y de dominio correctas (dinero con `Dinero`, parámetros del catálogo, sin `now()` en el dominio);
- aislamiento multi-tenant (sobre todo SQL nativo) y autorización (`@PreAuthorize`);
- que el contrato OpenAPI se actualizó si cambió la API y que `/api/v1` no se rompe;
- idioma y arquitectura (módulos, hexagonal en el núcleo);
- ausencia de secretos y de datos reales;
- documentación y `CLAUDE.md` al día;
- CI en verde, incluido el quality gate de SonarCloud.

Como autor: responde cada comentario, empuja los cambios a la misma rama y no resuelvas conversaciones ajenas. No se hace merge con el CI en rojo.

## Dependabot
Dependabot abre PR semanales hacia `staging` (Maven, Docker y GitHub Actions) con commits `chore(deps)` o `ci(deps)`. Qué hacer:
1. Espera a que el CI termine (sus PR no se analizan en SonarCloud, no tienen acceso a los secretos).
2. Lee las notas de versión de lo que sube; en cambios de versión menor o parche suele bastar con que el CI esté verde.
3. Si falla, arregla el código en la misma rama del PR o comenta `@dependabot rebase` tras el arreglo; si la actualización no es viable, ciérrala con un comentario del motivo.
4. Dependabot no propone saltos mayores de Spring Boot: los decide el equipo.
5. Un PR de dependencias pasa por la misma revisión que cualquier otro.

## Definition of Done
Un cambio está terminado cuando cumple **todo** lo siguiente (la plantilla del PR repite esta lista):

- [ ] Tests escritos y en verde (en el dominio, siguiendo TDD; ante un bug, con el test que lo reproduce).
- [ ] `./mvnw verify` pasa en local: compilación, tests, Checkstyle y Spotless.
- [ ] La cobertura (JaCoCo ≥ 80 %) y PIT (≥ 85 %) no bajan.
- [ ] Contrato `docs/openapi/openapi.json` regenerado si cambió la API, sin romper `/api/v1`.
- [ ] Documentación actualizada (README, `docs/`) y `CLAUDE.md` si cambia el mapa del proyecto.
- [ ] Commits convencionales y PR enlazado al issue.
- [ ] PR revisado y comentarios resueltos.
- [ ] CI en verde: **Build y tests**, **Escaneo de secretos**, **Contrato OpenAPI** y el quality gate de SonarCloud.
- [ ] Sin secretos ni datos reales; SQL nativo filtrando `tenant_id`.
