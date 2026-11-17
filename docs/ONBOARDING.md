# Onboarding del backend

Bienvenido/a al equipo de Nexo Software S.A.C. Esta guía te lleva desde un equipo vacío hasta tu primer PR en `planilla360-api`. No repite lo que ya está en otros documentos: cuando algo ya está explicado, te enlaza a él.

Los comandos están escritos para Git Bash (el terminal que instala Git for Windows) y, donde cambia, para PowerShell.

## 1. Día 1

### 1.1 Qué instalar
| Herramienta | Versión | Notas |
|---|---|---|
| Git | cualquiera reciente | incluye Git Bash |
| JDK | **21 (LTS)** | el build falla con otra versión; configura `JAVA_HOME` (ver abajo) |
| Docker Desktop | reciente | debe estar **encendido** para la base de datos local y para las pruebas de integración |
| IntelliJ IDEA | Community o Ultimate | *File → Project Structure → SDK* = 21 |
| Node.js | solo si vas a trabajar en el front | las versiones y los pasos están en el repo `planilla360-web` |

`JAVA_HOME` apunta a la carpeta del JDK 21:
- Git Bash: `export JAVA_HOME="/c/Program Files/Java/jdk-21"`
- PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"`

Ajusta la ruta a donde lo hayas instalado. Para que quede fijo, defínelo como variable de entorno del usuario en Windows (y abre un terminal nuevo).

Comprueba todo antes de seguir:
```bash
git --version
docker --version
docker info          # debe mostrar "Server:"; si no conecta, Docker Desktop está apagado
./mvnw -v            # debe decir "Java version: 21..."
```

### 1.2 Clonar, levantar y entrar
Los pasos exactos (clonar, `docker compose --profile full up -d`, `./mvnw spring-boot:run`, abrir Swagger y hacer login) están en el [README, «Inicio rápido»](../README.md#inicio-rápido); síguelos tal cual. Lo que debes lograr:

1. `docker compose ps` muestra los servicios en marcha (ejecutado en la raíz del repo, donde está `compose.yaml`).
2. La API arranca y `http://localhost:8080/actuator/health` responde `UP`.
3. En `http://localhost:8080/swagger-ui.html` ejecutas `POST /api/v1/auth/login` con `rrhh@textilesandinos.ejemplo.pe` y la contraseña común de desarrollo (README, «Usuarios de desarrollo»), copias el `accessToken`, pulsas *Authorize* y pruebas `GET /api/v1/empleados`.

Si algo falla, ve primero a la sección 4 («Problemas frecuentes»).

## 2. Recorrido del código

### 2.1 Cómo está organizado
- **Monolito modular** (Spring Modulith): un paquete por módulo bajo `src/main/java/pe/nexosoftware/planilla360` (`empresa`, `identidad`, `empleado`, `asistencia`, `vacaciones`, `planilla`, `boleta`, `notificacion`, `reporte`…). Un módulo solo usa la API pública de otro (las clases de su paquete raíz) o sus eventos; el interior es privado. La tabla completa está en el README, «Estructura del código».
- **Idioma**: el dominio va en español (clases, métodos de negocio, tablas, endpoints); la infraestructura en inglés (`Config`, `Filter`, sufijos `Controller`/`Service`/`Repository`).
- **Hexagonal solo en el núcleo** (`planilla`, `asistencia`, `vacaciones`): `domain/` (Java puro, sin Spring), `application/` (casos de uso en `port/in`, puertos en `port/out`, `service/`) e `infrastructure/` (persistencia, web, adaptadores). El resto de módulos usa capas clásicas.
- **Las reglas se hacen cumplir con pruebas**: `ModulithStructureTests` (límites entre módulos), `ArquitecturaTests` y `ArquitecturaHexagonalTests` (las dos clases ArchUnit están en `src/test/java/pe/nexosoftware/planilla360/arquitectura`; `ModulithStructureTests` está en la raíz de `src/test/java/pe/nexosoftware/planilla360`). Si rompes una regla, esas pruebas te dicen qué clase la incumple.
- **Multi-tenant por columna `tenant_id`**: el tenant sale del JWT. Un recurso de otra empresa responde 404. Las consultas SQL nativas no se filtran solas.
- **Dinero** siempre con `Dinero` (nunca `double`); el dominio no llama a `LocalDate.now()`.

### 2.2 Dónde mirar primero
Lee [CLAUDE.md](../CLAUDE.md): la sección «Convenciones» y la sección «Dónde vive qué» son el mapa del repo, módulo por módulo, con endpoints y clases clave. Después, cuando los necesites, [contrato-api.md](contrato-api.md) y [observabilidad.md](observabilidad.md).

### 2.3 Cómo seguir una petición
Ejemplo: `POST /api/v1/planillas/{id}/cerrar`.

1. **Controller** (`planilla/infrastructure/web/controller/PlanillaController`): recibe la petición, valida, comprueba el rol con `@PreAuthorize` y llama a un caso de uso. Recibe y devuelve DTO (records), nunca entidades.
2. **Caso de uso** (`planilla/application/port/in/CerrarPlanillaUseCase`, implementado en `application/service/CierrePlanillaService`): orquesta la transacción, carga la planilla por un puerto de salida y publica eventos.
3. **Dominio** (`planilla/domain/model/Planilla`): las reglas de negocio (por ejemplo `cerrar`, idempotente por clave) viven aquí, en Java puro.
4. **Persistencia** (`planilla/infrastructure/persistence/adapter`): el adaptador implementa el puerto `PlanillaPort` con repositorios JPA y entidades; el tenant se filtra con `@TenantId`.
5. **Errores**: las excepciones de negocio se convierten en `application/problem+json` en `GlobalExceptionHandler` (las del dominio, en 422).

Tip: en IntelliJ, *Go to Implementation* (`Ctrl+Alt+B`) sobre un puerto te lleva a su servicio, y *Find Usages* (`Alt+F7`) te lleva en sentido contrario.

### 2.4 Cómo leer un test
- **Dominio** (`src/test/.../planilla/domain/**`): pruebas unitarias con JUnit 5 y AssertJ, sin Spring ni base de datos; una clase corre en segundos (el primer arranque de Maven es más lento porque descarga dependencias). Empieza por `CalculadoraAsignacionFamiliarTests`: cada método `@Test` tiene nombre de regla de negocio (`esUnMontoPorTrabajadorNoPorHijo`), así que la lista de métodos es la especificación. Los `*Builder` de pruebas arman datos de ejemplo.
- **Escenarios Cucumber** (`src/test/resources/features/planilla/*.feature`): reglas en Gherkin en español, con sus pasos en `planilla/bdd/`.
- **Integración** (clases que extienden `soporte/IntegracionBase`): levantan la app completa con PostgreSQL real en Testcontainers; por eso necesitan Docker encendido. `PersonalBase` deja lista una empresa con empleados.
- Una prueba concreta: `./mvnw test -Dtest=CalculadoraAsignacionFamiliarTests` (PowerShell: `.\mvnw.cmd test '-Dtest=CalculadoraAsignacionFamiliarTests'`).

## 3. Tu primer issue y tu primer PR
El flujo completo (rama desde `staging`, Conventional Commits, PR hacia `staging`) está en [CONTRIBUTING.md](../CONTRIBUTING.md); léelo entero antes de empezar.

Consejos propios:
- Elige un primer issue pequeño y pregunta antes de programar si algo no está claro.
- En lógica de dominio, **test primero**: un commit `test:` que falla y luego `feat:`/`fix:` en verde.
- Corre `./mvnw verify` con JDK 21 y Docker encendido antes de abrir el PR (tarda varios minutos). Si tocas endpoints o DTO, actualiza `docs/openapi/openapi.json` ([contrato-api.md](contrato-api.md)).
- El merge no lo haces tú; la **Definition of Done** está en [CONTRIBUTING.md](../CONTRIBUTING.md#definition-of-done).

Un primer PR pequeño puede ser una prueba que falta, un mensaje de error o un párrafo de documentación. Nunca subas secretos, contraseñas ni datos reales de personas.

## 4. Problemas frecuentes

### 4.1 `docker` no se reconoce como comando
- **Síntoma**: tras instalar Docker Desktop, el terminal dice `docker : The term 'docker' is not recognized...` (PowerShell) o `bash: docker: command not found` (Git Bash).
- **Causa**: el terminal se abrió antes de la instalación y no conoce el `PATH` nuevo, o Docker Desktop no terminó de instalarse.
- **Cómo diagnosticarlo**: abre un terminal nuevo (cierra todos, también el de IntelliJ) y ejecuta `docker --version`. Comprueba que el ejecutable existe: en cmd, `where docker` debe listar, entre sus líneas, `C:\Program Files\Docker\Docker\resources\bin\docker.exe` (también puede listar una entrada sin extensión); en PowerShell, `Test-Path "C:\Program Files\Docker\Docker\resources\bin\docker.exe"`.
- **Solución**: reinicia el terminal (o el equipo) y repite `docker --version`. Si el ejecutable existe pero sigue sin reconocerse, añade `C:\Program Files\Docker\Docker\resources\bin` al `PATH` del usuario (*Variables de entorno*) y abre un terminal nuevo. Después abre Docker Desktop y espera a que el motor esté en marcha antes de usar `docker info`.

### 4.2 `docker compose` no encuentra el archivo
- **Síntoma**: `no configuration file provided: not found`.
- **Causa**: ejecutaste el comando fuera de la raíz del repo; `compose.yaml` está solo en la raíz de `planilla360-api`.
- **Cómo diagnosticarlo**: `pwd` (PowerShell: `Get-Location`) y `ls compose.yaml`. Si el archivo no aparece, estás en la carpeta equivocada.
- **Solución**: `cd` a la raíz del repo (la que contiene `compose.yaml`, `pom.xml` y `README.md`) y repite. También puedes indicar el archivo desde cualquier sitio: `docker compose -f /ruta/a/planilla360-api/compose.yaml --profile full up -d`. Para validar el archivo sin levantar nada: `docker compose config -q` (sin salida = correcto).

### 4.3 `Connection refused` a RabbitMQ
- **Síntoma**: al arrancar la API, o al calcular/cerrar una planilla y generar boletas, el log muestra errores de conexión a RabbitMQ con `Connection refused` hacia `127.0.0.1:5672` (la API conecta a `RABBITMQ_HOST`, por defecto `127.0.0.1`). `http://localhost:8080/actuator/health` sale `DOWN` con el detalle de `rabbit`.
- **Causa**: con el perfil local la API usa RabbitMQ (`MESSAGING_ENABLED=true`), pero levantaste solo `core` o `storage`, que no lo incluyen. RabbitMQ está en los perfiles `messaging` y `full`.
- **Cómo diagnosticarlo**: `docker compose ps` (¿aparece `rabbitmq` y está `healthy`?). Comprueba los puertos: la consola en http://localhost:15672 (usuario `planilla360`, clave `planilla360`) y AMQP en el `5672` (PowerShell: `Test-NetConnection localhost -Port 5672`).
- **Solución**: `docker compose --profile full up -d` (o `--profile messaging`), espera a `healthy` en `docker compose ps` y reinicia la API. Si solo quieres la base de datos, arranca la API sin mensajería (`MESSAGING_ENABLED=false STORAGE_TIPO=memoria INTEGRACION_MODO=stub`; ver «Interruptores» en el README).

### 4.4 El puerto 8080 está ocupado
- **Síntoma**: la API no arranca y el log dice `Web server failed to start. Port 8080 was already in use.`
- **Causa**: otro proceso escucha en el 8080 (una API anterior que no cerraste u otra aplicación).
- **Cómo diagnosticarlo**: en PowerShell o cmd, `netstat -ano | findstr :8080`. La línea con `LISTENING` termina en el PID del proceso, por ejemplo `TCP    0.0.0.0:8080    0.0.0.0:0    LISTENING    6920` (con Spring suele verse `0.0.0.0:8080` o `[::]:8080`). Para ver qué programa es: `tasklist /FI "PID eq 6920"` (en PowerShell, `tasklist /FI 'PID eq 6920'`).
- **Solución**: si el proceso es tuyo y puedes cerrarlo, `taskkill /PID <pid> /F` y vuelve a arrancar. Si no, cambia el puerto de la API con la variable `PORT`: `PORT=8081 ./mvnw spring-boot:run` (PowerShell: `$env:PORT = "8081"; .\mvnw.cmd spring-boot:run`) y abre Swagger en el puerto nuevo. Ojo: el front espera la API en el 8080 por defecto.

### 4.5 Maven usa un Java que no es el 21
- **Síntoma**: `./mvnw` falla con `Planilla360 requiere JDK 21 (LTS). Configura JAVA_HOME o el SDK del proyecto en IntelliJ.` (regla `RequireJavaVersion` de maven-enforcer).
- **Causa**: tu `java` por defecto es otra versión (por ejemplo 26) y `JAVA_HOME` no apunta al JDK 21.
- **Cómo diagnosticarlo**: `./mvnw -v` y mira las líneas `Java version:` y `runtime:`; también `echo $JAVA_HOME` (PowerShell: `$env:JAVA_HOME`).
- **Solución**: apunta `JAVA_HOME` al JDK 21 (sección 1.1) y repite `./mvnw -v` hasta ver `Java version: 21`. En IntelliJ, además, *File → Project Structure → SDK* = 21. Comprobación rápida: `./mvnw -q validate` (sin salida = correcto).

### 4.6 Las pruebas de integración fallan por Docker apagado
- **Síntoma**: las clases de integración (las que extienden `IntegracionBase`) fallan al inicio con errores de Testcontainers (entorno Docker no encontrado), mientras las pruebas de dominio sí pasan.
- **Causa**: Docker Desktop está apagado o aún arrancando; Testcontainers necesita levantar PostgreSQL.
- **Cómo diagnosticarlo**: `docker info`. Si falla con un error de conexión al motor, Docker no está listo; si responde con una sección `Server:`, sí lo está.
- **Solución**: abre Docker Desktop, espera a que el motor esté en marcha, comprueba `docker info` y repite. Mientras tanto puedes correr solo el dominio, que no necesita Docker: `./mvnw test -Dtest='*Calculadora*'`.

### 4.7 La API no autentica en PostgreSQL tras recrear los contenedores
- **Síntoma**: la API falla con `password authentication failed for user "planilla360"` después de cambiar claves y recrear los contenedores.
- **Causa**: compose y la API leen las credenciales por separado. Compose toma `POSTGRES_PASSWORD` de tu `.env`; la API en el host **no lee `.env`** y usa `DB_PASSWORD` (por defecto `planilla360`). Copiar `.env.example` tal cual es seguro (sus valores coinciden con los por defecto de la API), pero si cambiaste una clave solo en `.env`, compose creó la base con la nueva y la API sigue con la de por defecto. Además, el volumen `postgres-data` conserva la clave con la que se creó la primera vez.
- **Cómo diagnosticarlo**: `docker compose --profile core config` (sin perfil no muestra servicios) muestra el valor efectivo de `POSTGRES_PASSWORD`; compáralo con el `DB_PASSWORD` con el que arrancas la API y revisa tu `.env`.
- **Solución**: copiar `.env.example` entero es seguro en local. Lo que cambies de una pareja debe cambiarse en las dos puntas:
  - **Compose lee `.env`** (en la raíz del repo).
  - **La API en el host NO lee `.env`**: toma variables de entorno exportadas en la consola donde haces `./mvnw spring-boot:run` (Git Bash: `export DB_PASSWORD=...`; PowerShell: `$env:DB_PASSWORD = "..."`) o en las variables de entorno de la configuración de ejecución de IntelliJ. `RABBITMQ_USER` y `RABBITMQ_PASSWORD` las leen ambos, con el mismo nombre.

    | Servicio | Compose (`.env`) | API (consola o IntelliJ) |
    |---|---|---|
    | PostgreSQL | `POSTGRES_USER` | `DB_USER` |
    | PostgreSQL | `POSTGRES_PASSWORD` | `DB_PASSWORD` |
    | PostgreSQL | `POSTGRES_PORT` y `POSTGRES_DB` | `DB_URL` (mismo puerto y nombre de base) |
    | RabbitMQ | `RABBITMQ_USER` y `RABBITMQ_PASSWORD` | `RABBITMQ_USER` y `RABBITMQ_PASSWORD` |
    | MinIO | `MINIO_ROOT_USER` | `STORAGE_ACCESS_KEY` |
    | MinIO | `MINIO_ROOT_PASSWORD` | `STORAGE_SECRET_KEY` |

  Para volver al estado de fábrica, borra el `.env`, abre un terminal nuevo (sin esas variables exportadas) y sigue «Reiniciar la base local desde cero» del README (`docker compose --profile full down -v`, levantar de nuevo y arrancar la API). `down -v` borra la base local, las colas y los archivos de MinIO.

## 5. A quién preguntar
Primero el README, CLAUDE.md y esta guía; después, tu mentor asignado en el tablero. Si encuentras un error en esta guía, corrígelo con un PR `docs:`.
