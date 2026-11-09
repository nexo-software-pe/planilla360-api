# Contrato de la API (OpenAPI)

El contrato público de la API vive **versionado en el repositorio**: [`docs/openapi/openapi.json`](openapi/openapi.json). Es lo que ven los clientes (el front, integraciones de las empresas) y no puede cambiar por accidente.

## Cómo se protege

1. **Test `ContratoOpenApiTests`** (corre en `./mvnw verify` y en el CI): levanta la aplicación, pide `/v3/api-docs` y exige que sea idéntico al archivo versionado. Si cambias un endpoint, un parámetro, un DTO o una validación sin actualizar el archivo, el test falla y te dice cómo regenerarlo.
2. **Job `Contrato OpenAPI` del CI** (solo en pull requests): compara el contrato de tu rama con el de la rama destino (`staging` o `main`) usando [oasdiff](https://github.com/oasdiff/oasdiff) y **falla si el cambio rompe la compatibilidad**.

Juntos cubren los dos descuidos posibles: cambiar el código sin tocar el contrato (lo detecta el test) y cambiar el contrato rompiendo a los clientes (lo detecta oasdiff).

## Qué cuenta como cambio que rompe

oasdiff clasifica los cambios; en nuestro caso falla con cualquier hallazgo de nivel `ERR`. Los más comunes:

- Quitar o renombrar un endpoint, un método HTTP o un campo de la respuesta.
- Cambiar el tipo de un campo, o convertir uno opcional en obligatorio en una petición.
- Añadir un parámetro obligatorio a un endpoint existente.
- Estrechar lo que se acepta en una petición (quitar un valor de un `enum` de entrada, bajar un `maxLength`).

La lista completa de reglas está en la documentación de oasdiff; ante la duda, el CI dice qué regla se incumple.

**No** rompen: añadir endpoints nuevos, añadir campos opcionales a una petición o campos a una respuesta, documentar mejor.

## Cómo hacer un cambio incompatible: versionar

`/api/v1` es una promesa. Si de verdad necesitas un comportamiento incompatible:

1. **No lo cambies en `/api/v1`.** Deja v1 como está (puedes marcarlo `deprecated`).
2. Publica la versión nueva en `/api/v2/...` (controller nuevo o prefijo nuevo). Para el contrato es un cambio aditivo, así que el CI pasa.
3. Anuncia el retiro de v1 con fecha y retíralo en un cambio aparte (ese sí romperá el contrato y requerirá la decisión del equipo).

## Cómo actualizar el contrato tras un cambio intencional

```bash
./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true
```

Revisa el diff de `docs/openapi/openapi.json` antes de hacer commit: ese diff es exactamente lo que cambia para los clientes. El archivo se genera normalizado (claves ordenadas, sin la URL del servidor) para que los diffs sean pequeños y estables.

En Windows, desde PowerShell: `.\mvnw.cmd test "-Dtest=ContratoOpenApiTests" "-Dcontrato.actualizar=true"`.

## Detalles del CI

- oasdiff se descarga con versión fija y se verifica su `sha256` antes de ejecutarlo; para actualizarlo hay que cambiar `OASDIFF_VERSION` y `OASDIFF_SHA256` en `.github/workflows/ci.yml` (el hash está en el `checksums.txt` de la release).
- El primer PR que introdujo el contrato no tenía contra qué compararse: si la rama destino no tiene el archivo, el job avisa y termina en verde.
- Para que este check sea **obligatorio** para fusionar, añade «Contrato OpenAPI» a los checks requeridos de la protección de ramas `staging` y `main`.
