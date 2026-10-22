# Roles de Pago — Backend (COOPSANJOSE LTDA)

Sistema para publicar y consultar roles de pago. RRHH sube los PDF de forma masiva, los publica, y cada empleado
consulta **solo su propio rol**. Al publicar, los empleados reciben un aviso por correo (sin datos del rol).
Todo acceso y toda acción quedan en una bitácora de auditoría.

- **Stack:** Spring Boot 4.1 · Java 21 · MySQL 8.4 · Flyway · Spring Security 7 · Vault
- **Zona horaria:** America/Guayaquil
- **Raíz de la API:** `http://localhost:8080/api/v1/roles-pago`

## 1. Arranque en local

Requisitos: JDK 21, Docker, y un archivo `.env` en la raíz (nunca se sube a Git).

```bash
sh docker/dev-up.sh          # MySQL + Vault de desarrollo (la primera vez tarda un poco)
```

Luego ejecuta la clase `RolPagosBackendFinalApplication` desde IntelliJ (o `./mvnw spring-boot:run`).

Comprobación: `GET http://localhost:8080/api/v1/roles-pago/actuator/health` debe responder `UP`.

Variables del `.env` (ejemplo, sin valores): `DB_NAME`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`,
`VAULT_DEV_ROOT_TOKEN`, `VAULT_PORT`, `AD_URL_BASE`, `AD_TOKEN`.

### Perfiles

| Perfil | Para qué | Correo |
|---|---|---|
| `dev` (por defecto) | Desarrollo y pruebas | **Simulado**: el aviso se escribe en el log, no sale nada |
| `dev,demo` | Presentación con correo real | **Real, pero redirigido a una sola dirección** (la tuya) |

Para el perfil `demo` agrega al `.env` (la aplicación **no arranca** si falta alguna: es a propósito, para no escribirle a empleados por accidente):

```
CORREO_API_URL=<URL completa del envío masivo de la API de correo>
CORREO_REDIRIGIR_A=<tu dirección de correo>
```

y en la configuración de ejecución de IntelliJ define la variable de entorno `SPRING_PROFILES_ACTIVE=dev,demo`.

## 2. Cómo entrar como ADMIN, RRHH o EMPLEADO

El backend no tiene pantalla de login: la identidad la entrega el **autenticador** (AD + Google Authenticator, token JWT RS256)
y este sistema valida el token y **decide los roles desde su propia base de datos** en cada petición.

| Modo | Cuándo usarlo | Cómo |
|---|---|---|
| **A. Con el frontend** | Login real | Iniciar sesión con AD y Google Authenticator. El token viaja en `Authorization: Bearer`. La clave pública del autenticador va en `certs/jwt-public.pem` (se ignora en Git) |
| **B. Cabeceras de desarrollo** | Demostración con Postman | `X-Dev-Usuario: <código AD>`. Sin más, los roles son los **reales** de la base de datos. Solo existe con el perfil `dev` |
| **C. Token local** | Probar el camino JWT sin el autenticador | `sh scripts/token-dev.sh <usuario>` genera un token firmado con una clave local; agrega a `.env`: `JWT_PUBLIC_KEY_FILE=./certs/dev/jwt-dev-public.pem` |

Para probar que **solo** el JWT abre la puerta (sin el modo B), ejecuta con `-Dapp.seguridad.dev-auth-habilitado=false`.

### Roles

| Rol | Quién lo tiene | Qué puede |
|---|---|---|
| `EMPLEADO` | Todo usuario activo del AD | Consultar **su propio** rol publicado |
| `RRHH` | Lo concede un ADMIN | Cargar, publicar y revisar roles; ver avisos y cobertura |
| `ADMIN` | `fsepa` y `msavala` (configuración) y quien se designe | Todo lo anterior, más usuarios, directorio y auditoría |
| Permiso `VER_ROLES_AJENOS` | ADMIN siempre; RRHH solo si un ADMIN se lo concede | Ver el rol de otra persona, con motivo obligatorio (10 a 200 caracteres) |

## 3. Guion de demostración (con Postman)

Importa `roles-pago-demo.postman_collection.json`. En la pestaña **Variables** de la colección ajusta:
`usuarioAdmin` (ya trae `fsepa`), `usuarioRrhh` y `usuarioEmpleado` (códigos AD de dos personas del directorio),
`cedulaEmpleado` (la cédula de `usuarioEmpleado`) y `periodo` (por ejemplo `2026-09`).

| # | Carpeta | Qué se demuestra | Resultado esperado |
|---|---|---|---|
| 1 | Sesión | `mi-perfil` de cada rol; sin sesión | 200 con roles distintos; 401 sin cabecera |
| 2 | Administración | Sincronizar el directorio, dar el rol RRHH, RRHH no entra a `/admin` | 200 · 200 · 403 |
| 3 | PDFs de demostración | Genera PDFs coherentes con el directorio (solo desarrollo) | 200, con la carpeta `data/demo-pdfs/<periodo>/` |
| 4 | RRHH | Carga masiva (seleccionar los PDF de la carpeta), resumen, publicar, avisos, cobertura, ver un rol ajeno con motivo | 200 en cada paso |
| 5 | Empleado | Ver períodos, consultar su rol, intentar con una cédula ajena | 200 · 200 · 403 |
| 6 | Auditoría | Accesos, acciones, intentos denegados, exportación CSV | 200 |

Tras publicar, el despachador envía los avisos en menos de un minuto (cada 30 s; 10 s en el perfil `demo`). Con el perfil `demo`
el correo llega a la dirección de `CORREO_REDIRIGIR_A`, marcado `[PRUEBA]`.

## 4. Dónde se cumple cada requisito

| Requisito | Dónde |
|---|---|
| RRHH sube PDF de forma masiva (nombre = cédula) | `POST /rrhh/periodos/{periodo}/roles-pago` (valida nombre del archivo, PDF, mes del período y nombre del titular contra el AD) |
| El empleado consulta solo su rol | `POST /mi-rol-pago/consultar` (la cédula debe ser la de su sesión; 5 intentos ajenos en 15 minutos bloquean con 429) |
| ADMIN y RRHH ven roles ajenos, con permiso y motivo | `POST /rrhh/periodos/{periodo}/roles-pago/{id}/ver` |
| Aviso por correo al publicar | Cola de avisos, despachador cada 30 s, reintentos; `GET /rrhh/periodos/{periodo}/notificaciones` |
| Auditoría completa | `GET /admin/auditoria/...` (accesos, acciones, denegados, exportación CSV) |
| Login con AD + Google Authenticator + JWT | Autenticador externo; aquí se valida el token (RS256, sin `type` MFA, vida máxima) |
| Módulos ADMIN, RRHH y EMPLEADO | `GET /mi-perfil` y las reglas de ruta (`/admin/**`, `/rrhh/**`) |

## 5. Endpoints (27)

Prefijo: `/api/v1/roles-pago`. Todas las respuestas, incluso los errores, tienen la forma
`{status, message, content, timestamp, success}`.

| Quién | Método y ruta |
|---|---|
| Cualquiera con sesión | `GET /mi-perfil` |
| Empleado | `GET /mi-rol-pago/periodos` · `POST /mi-rol-pago/consultar` |
| RRHH y ADMIN | `GET /rrhh/periodos` · `POST /rrhh/periodos/{p}/roles-pago` (multipart, campo `archivos`) · `GET /rrhh/periodos/{p}/roles-pago` · `POST .../roles-pago/{id}/inactivar` · `POST .../roles-pago/{id}/activar` · `POST .../roles-pago/{id}/ver` · `GET /rrhh/periodos/{p}/resumen` · `POST /rrhh/periodos/{p}/publicar` · `GET /rrhh/periodos/{p}/cobertura` · `GET /rrhh/periodos/{p}/notificaciones` · `POST .../notificaciones/generar` · `POST .../notificaciones/reintentar` |
| ADMIN | `POST /admin/directorio/sincronizar` · `GET /admin/directorio/estado` · `POST /admin/directorio/buscar` · `GET /admin/directorio/usuarios/{username}` · `GET /admin/usuarios` · `GET /admin/usuarios/privilegiados` · `POST /admin/usuarios/{username}/roles/{ADMIN\|RRHH}/otorgar` · `POST .../revocar` · `GET /admin/auditoria/accesos` · `GET .../acciones` · `GET .../denegados` · `GET .../accesos/exportar` |
| Solo perfil `dev` | `POST /prueba/demo/pdfs` (genera PDFs de demostración) y los de `/prueba/*` |

El período se escribe siempre `yyyy-MM`. La cédula nunca viaja en una URL. Los PDF (y el CSV) viajan en base64 dentro del JSON.

## 6. Seguridad en resumen

- Cerrado por defecto: solo `/actuator/health` y `/error` son públicos.
- Los roles **no** vienen en el token: salen de la base de datos en cada petición, así que revocar un rol o dar de baja a alguien en el AD tiene efecto inmediato.
- Se aceptan solo tokens RS256 de sesión; se rechazan el token temporal del paso MFA, los de vida de renovación, los sin firma y los firmados con otra clave. Las cookies se ignoran.
- Las cédulas salen enmascaradas; los registros no contienen cédulas, tokens ni direcciones de correo.
- Sin borrado físico: todo se inactiva y queda auditado.

## 7. Pruebas

```bash
./mvnw test        # las de integración (*IT) necesitan MySQL y Vault levantados (sh docker/dev-up.sh)
```

Cubren la carga y publicación, las reglas de acceso, el validador JWT, el límite de intentos, la cola de correo y la auditoría.

## 8. Solución de problemas

| Síntoma | Causa probable |
|---|---|
| La aplicación no arranca: «No hay forma de autenticar usuarios» | No hay clave JWT (`certs/jwt-public.pem`) y el modo de desarrollo está apagado |
| Con el perfil `demo` no arranca: «Could not resolve placeholder» | Falta `CORREO_API_URL` o `CORREO_REDIRIGIR_A` en el `.env` |
| 401 en todo | Falta `X-Dev-Usuario` (modo B) o el token no es válido (revisa el log: dice el motivo sin mostrar el token) |
| 403 | El usuario no tiene ese rol; se lo da un ADMIN con `.../roles/RRHH/otorgar` |
| 429 al consultar | Demasiadas cédulas ajenas tecleadas; se libera a los 15 minutos |
| Un PDF es rechazado | Revisa: nombre `<cédula>.pdf`, que sea un PDF válido, que traiga «MES AÑO» del período y el nombre del titular |
| El correo no llega | Mira `GET .../notificaciones`: el estado y el número de intentos; en el perfil `dev` el envío es simulado (log) |

## 9. Pendiente para producción (no forma parte de esta entrega)

Datos de Vault y despliegue, dominio del frontend (CORS), clave de firma propia de producción en el autenticador y las correcciones
de su `/auth/refresh` y su cierre de sesión, y la plantilla de correo definitiva de la API. La plantilla de configuración está en
`src/main/resources/application-prod.yml` (todo comentado) y existe un `Dockerfile` listo.
