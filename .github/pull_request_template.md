## Qué cambia
<!-- Resumen breve del cambio -->

Cierra #

## Cómo se probó
<!-- Tests agregados/ejecutados, pasos manuales -->

## Checklist ([Definition of Done](https://github.com/nexo-software-pe/planilla360-api/blob/staging/CONTRIBUTING.md#definition-of-done))
- [ ] Tests escritos y en verde (ante un bug, con el test que lo reproduce)
- [ ] `./mvnw verify` pasa en local (compilación, tests, Checkstyle y Spotless)
- [ ] La cobertura (JaCoCo ≥ 80 %) y PIT (≥ 85 %) no bajan
- [ ] Contrato `docs/openapi/openapi.json` regenerado si cambia la API (sin romper `/api/v1`)
- [ ] Documentación y `CLAUDE.md` actualizados si cambia el mapa del proyecto
- [ ] Commits siguen Conventional Commits
- [ ] Sin secretos ni datos reales; el SQL nativo filtra `tenant_id`
- [ ] PR revisado y CI en verde (Build y tests, Escaneo de secretos, Contrato OpenAPI, SonarCloud)
