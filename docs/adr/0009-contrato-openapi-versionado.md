# ADR 0009: Contrato OpenAPI versionado

- **Estado:** Aceptado
- **Fecha:** 2026-11-09 (Fase 6, contrato)

## Contexto

La API la consumen el front y las integraciones de las empresas. Un cambio de endpoint, parámetro, DTO o validación que no se advierte puede romper a esos clientes sin que ninguna prueba unitaria falle. Hay dos descuidos distintos: cambiar el código sin reflejarlo en el contrato, y cambiar el contrato rompiendo la compatibilidad.

## Decisión

- **Contrato en el repositorio.** `docs/openapi/openapi.json` está versionado y se genera normalizado (claves ordenadas, sin la URL del servidor) para que los diffs sean pequeños.
- **Test del contrato.** `ContratoOpenApiTests` (corre en `./mvnw verify` y en el CI) levanta la aplicación, lee `/v3/api-docs` y exige que sea idéntico al archivo. Tras un cambio intencional se regenera con `./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true` y se revisa el diff.
- **Comparación en el CI.** El job `Contrato OpenAPI` (solo en pull requests) compara el contrato de la rama con el de la rama destino usando oasdiff con versión fija y `sha256` verificado, y falla ante cualquier hallazgo de nivel `ERR`. `.github/oasdiff-niveles.txt` eleva a `ERR` reglas que oasdiff trata como `WARN` por defecto pero que rompen a los clientes (quitar propiedades de respuesta, volver obligatorio un parámetro o campo de petición, cambiar tipos, quitar valores de un enum de respuesta).
- **`/api/v1` no se rompe.** Un cambio incompatible no se hace en v1: se publica en `/api/v2` (cambio aditivo para el contrato), se anuncia el retiro de v1 con fecha y se retira en un cambio aparte que sí requiere decisión del equipo.

Procedimiento completo en `docs/contrato-api.md`.

## Consecuencias

- Todo cambio de la API es visible en el diff del PR y no puede ocurrir por accidente.
- Añadir endpoints o campos opcionales no rompe; quitar, renombrar o endurecer sí falla el CI.
- Mantener dos versiones en paralelo tiene costo; se acepta a cambio de no romper clientes.
- Actualizar oasdiff exige cambiar versión y hash en el workflow.
- Para que el job bloquee la fusión debe ser un check requerido en la protección de ramas; eso es una decisión de gobierno del repositorio, aparte de este ADR.
- Si la rama destino no tiene el archivo (el primer PR que introdujo el contrato), el job avisa y termina en verde.

## Alternativas consideradas

- **Contrato generado sin versionar:** no hay diff que revisar ni base de comparación.
- **Solo el test de igualdad:** detecta el cambio no declarado, pero no distingue uno compatible de uno que rompe.
- **Contract testing por consumidor (p. ej. Pact):** más preciso, pero exige coordinar con cada consumidor; hoy el contrato público único es suficiente.
- **Versionado por cabecera o por fecha:** menos visible y más difícil de documentar que el prefijo `/api/vN`.
- **Reglas de oasdiff con niveles por defecto:** dejan pasar como aviso cambios que rompen a los clientes.
