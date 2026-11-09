# ADR 0001 — Despliegue, releases y servicios en la nube

Estado: aceptada (Fase 9)

## Contexto
Hay que publicar la API en dos entornos (staging y producción) usando solo planes gratuitos, con el mismo flujo en los dos repos: un merge a `staging` despliega staging y un merge a `main` despliega producción, con etiqueta de versión y CHANGELOG.

## Decisión
1. **La imagen se construye en GitHub Actions, no en Render.** El job `Imagen Docker y Trivy` la construye, la escanea y, solo si pasa, la publica en GitHub Container Registry (`ghcr.io/nexo-software-pe/planilla360-api`). La imagen desplegada es exactamente la que se analizó. Compilar con Maven dentro del plan gratuito de Render (poca memoria y tiempo) sería lento y frágil.
2. **Render ejecuta una imagen existente** (servicios `Existing Image`, sin auto-deploy). El job `Desplegar en Render` llama al *deploy hook* del servicio con `imgURL=…:sha-<commit>`: se despliega un commit concreto, no «lo último de la rama». Solo corre si el build, el escaneo de secretos y la imagen pasaron.
3. **Dos servicios y dos bases separados**: `planilla360-api-staging` y `planilla360-api` (producción); un proyecto de Neon por entorno (credenciales y datos aislados) y un bucket de R2 por entorno.
4. **Secretos solo en los paneles y en GitHub** (entornos `staging` y `production`): el hook de Render es un secreto del entorno; la URL de la API es una variable (no es secreta).
5. **Versionado**: la versión vive en `pom.xml`. Un PR hacia `main` es un release y el job `Versión y CHANGELOG` exige una versión final nueva (sin etiqueta previa) y su sección en `CHANGELOG.md`. Tras desplegar producción, el job `Etiqueta y release` crea `vX.Y.Z` y el release de GitHub con las notas del CHANGELOG.
6. **Degradación con flags en la nube**: sin RabbitMQ (`MESSAGING_ENABLED=false`: boletas y correos se procesan en el mismo proceso, sin reintentos ni DLQ), sin Redis (`CACHE_TYPE=simple`) y sin correo (`NOTIFICACIONES_HABILITADAS=false`). Los archivos sí van a Cloudflare R2.
7. **Primer administrador**: los entornos nuevos no tienen datos de desarrollo, así que `AdministradorInicial` crea un `ADMIN_SAAS` al arrancar desde `ADMIN_INICIAL_EMAIL` y `ADMIN_INICIAL_PASSWORD` (variables del panel). Es idempotente y nunca cambia una clave existente.

## Alternativas consideradas
- **Render construye desde el repo (Dockerfile):** más simple de configurar, pero compila en la máquina gratuita y despliega aunque el CI falle (o hay que gestionar `Auto-Deploy: After CI checks pass`, que no distingue Trivy ni el contrato).
- **`release-please` u otra herramienta que abre PR de release:** automatiza versión y changelog, pero los PR que crea el `GITHUB_TOKEN` no disparan los checks obligatorios. Con un solo desarrollador es más claro subir la versión a mano en el PR de release y que el CI lo valide.
- **RabbitMQ (CloudAMQP), Redis (Upstash) y correo (Brevo):** existen planes gratuitos, pero cada uno suma una cuenta y credenciales para una carga mínima. Los flags de la aplicación permiten añadirlos después sin cambiar código.

## Consecuencias
- Render free duerme tras 15 minutos sin tráfico: la primera petición tarda cerca de un minuto. Neon free suspende la base a los 5 minutos. El job de despliegue espera hasta 10 minutos a que la API responda.
- Sin RabbitMQ en la nube no se prueban allí los reintentos ni la DLQ (sí en local y en los tests de integración).
- La imagen es pública para que Render la descargue sin credenciales; el repositorio ya es público y no contiene secretos.
