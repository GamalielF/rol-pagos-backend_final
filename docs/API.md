# Contrato de la API — Roles de Pago

Raíz en desarrollo: `http://localhost:8080/api/v1/roles-pago`. Todas las rutas de este documento cuelgan de esa raíz.
Los tipos TypeScript equivalentes están en la carpeta `front-models/` (un archivo por concepto, con `index.ts`).

## 1. Convenciones

**Autenticación.** Cabecera `Authorization: Bearer <token del autenticador>` en cada petición. Las cookies se ignoran.
Los **roles no vienen en el token**: el servidor los lee de su base de datos en cada petición; el front los obtiene de `GET /mi-perfil`
y los usa solo para mostrar u ocultar menús (la seguridad real la aplica siempre el servidor).

**Respuesta.** Toda respuesta, también los errores, tiene esta forma:

```json
{ "status": "200", "message": "Perfil del usuario", "content": { }, "timestamp": "2026-10-22T16:53:39.558", "success": true }
```

En un error no hay `content`, `success` es `false`, y las validaciones traen `errors` con un mensaje por campo. El `message` de los
errores 400, 403, 404, 409 y 429 está redactado para mostrarse tal cual al usuario.

**Formatos.**

| Tipo | Formato | Ejemplo |
|---|---|---|
| Período | `yyyy-MM` | `2026-09` |
| Fecha | `yyyy-MM-dd` | `2026-10-22` |
| Fecha y hora | ISO-8601 sin zona, hora de Ecuador | `2026-10-22T16:53:39` |
| Cédula | 10 dígitos. **Nunca va en una URL**; en las respuestas sale enmascarada | `17******75` |
| Archivos (PDF, CSV) | Base64 dentro del JSON (`contenidoBase64`) | |

**Paginación.** Parámetros `pagina` (base 0, por defecto 0) y `tamano` (por defecto 50). La respuesta es:

```json
{ "elementos": [], "pagina": 0, "tamano": 50, "totalElementos": 0, "totalPaginas": 0 }
```

`<ui-data-table>` espera otro formato y otros nombres (`page`, `size`, `content`...): use `toSpringPage` y `toParametrosApi` de `paginacion.util.ts`.

## 2. Errores

| Código | Cuándo | Qué hace el front |
|---|---|---|
| 400 | Datos inválidos (cédula que no tiene 10 dígitos, período mal escrito, motivo corto...) | Mostrar `errors` junto a cada campo |
| 401 | Sin sesión o token inválido, vencido o de otro tipo | Llevar al login (el interceptor de Lumen ya intenta renovar una vez) |
| 403 | Sin permiso, o cédula que no es la suya | Mostrar `message` |
| 404 | El período, el rol o el usuario no existe | Mostrar `message` |
| 405, 415 | Método o tipo de contenido equivocado | Error de programación |
| 409 | Conflicto: período ya publicado, operación no permitida, o dato modificado por otra persona | Mostrar `message`; recargar si fue un cambio concurrente |
| 413 | Un archivo o la petición superan el límite | Mostrar `message` |
| 429 | Demasiadas cédulas ajenas tecleadas (5 en 15 minutos) | Mostrar `message` y bloquear el formulario un momento |
| 502 | Un servicio externo (el AD) no responde | Mostrar `message`; reintentar después |
| 500 | Error inesperado; el `message` es genérico y no revela detalles | Mostrar «intente más tarde» |

## 3. Perfil y empleado (cualquier persona con sesión)

| Método y ruta | Entrada | Respuesta (`content`) |
|---|---|---|
| `GET /mi-perfil` | — | `PerfilResponse`: `username`, `nombre`, `cargo`, `cedulaEnmascarada`, `puedeConsultarSuRol`, `roles` (del más al menos privilegiado), `permisos` |
| `GET /mi-rol-pago/periodos` | — | `string[]`: períodos **publicados** disponibles, `yyyy-MM`, del más reciente al más antiguo |
| `POST /mi-rol-pago/consultar` | `{ "cedula": "…", "periodo": "2026-09" }` (`periodo` es opcional: sin él, el más reciente) | `RolPagoPdfResponse` con el PDF en base64 |

La cédula debe ser la de la sesión. Cada intento con una cédula ajena queda auditado, y al quinto en 15 minutos se responde 429 aunque la siguiente sea la correcta.
Quien no tiene cédula en el directorio recibe `puedeConsultarSuRol: false` en `mi-perfil`: no muestre el formulario.

## 4. RRHH (roles `RRHH` y `ADMIN`)

El `{periodo}` es `yyyy-MM`; `{id}` es el `id` del `RolPagoResumenResponse`.

| Método y ruta | Entrada | Respuesta |
|---|---|---|
| `GET /rrhh/periodos` | — | `PeriodoResumenResponse[]`, del más reciente al más antiguo |
| `POST /rrhh/periodos/{periodo}/roles-pago` | `multipart/form-data`, campo `archivos` repetido (un PDF por archivo) | `ResultadoCargaResponse` con el resultado de **cada** archivo |
| `GET /rrhh/periodos/{periodo}/roles-pago` | — | `ListadoPeriodoResponse` |
| `POST /rrhh/periodos/{periodo}/roles-pago/{id}/inactivar` | — | `RolPagoResumenResponse` |
| `POST /rrhh/periodos/{periodo}/roles-pago/{id}/activar` | — | `RolPagoResumenResponse` |
| `POST /rrhh/periodos/{periodo}/roles-pago/{id}/ver` | `{ "motivo": "…" }` (10 a 200 caracteres) | `RolPagoPdfResponse` |
| `GET /rrhh/periodos/{periodo}/resumen` | — | `ResumenPeriodoResponse`: cuadre de personas activas contra roles, en los dos sentidos |
| `POST /rrhh/periodos/{periodo}/publicar` | opcional `{ "confirmarFaltantes": true }` | `PublicacionResponse` |
| `GET /rrhh/periodos/{periodo}/cobertura` | — | `CoberturaPeriodoResponse`: quién ya abrió su rol |
| `GET /rrhh/periodos/{periodo}/notificaciones` | — | `InformeNotificacionesResponse` |
| `POST /rrhh/periodos/{periodo}/notificaciones/generar` | — | `ResultadoEncoladoResponse` (no duplica los ya creados) |
| `POST /rrhh/periodos/{periodo}/notificaciones/reintentar` | — | `ResultadoReintentoResponse` |

**Carga masiva.**
- El nombre de cada archivo es `<cédula de 10 dígitos>.pdf`. Cada PDF se valida: que lo sea, que traiga el mes y año del período y que contenga el nombre del titular según el AD.
- Un archivo malo **no** detiene el lote: aparece como `RECHAZADO` con su `mensaje`. Los estados son `CARGADO`, `REEMPLAZADO` (ya existía y cambió), `SIN_CAMBIOS` y `RECHAZADO`.
- Límites: 5 MB por archivo, 100 MB por petición y 500 archivos. **Recomendado: lotes de hasta 100 archivos y 80 MB.**

**Publicar.** Si el cuadre tiene diferencias (personas activas sin rol, o roles sin persona activa), el servidor no publica y responde con un error cuyo
`message` las explica; el usuario debe confirmar y se reenvía con `confirmarFaltantes: true`. Al publicar se encolan los avisos por correo (`avisosEncolados`).

**Ver el rol de otra persona.** `ADMIN` siempre puede. `RRHH` necesita el permiso `VER_ROLES_AJENOS` (403 si no lo tiene; lo concede un ADMIN). El motivo queda en la auditoría.

**Avisos.** Estados: `PENDIENTE` (por enviar o esperando reintento), `ENVIADA` (aceptada por la API de correo), `FALLIDA` (agotó los intentos o la dirección es inválida) y `OMITIDA` (la persona no tiene cuenta activa).
`problemas` lista los avisos con fallos, con nombre y no con correo. La cola se despacha sola cada 30 segundos.

## 5. Administración (rol `ADMIN`)

| Método y ruta | Entrada | Respuesta |
|---|---|---|
| `GET /admin/usuarios` | `buscar` (texto opcional), `pagina`, `tamano` | `PaginaResponse<UsuarioAdminResponse>` |
| `GET /admin/usuarios/privilegiados` | — | `UsuarioAdminResponse[]` (quienes tienen ADMIN o RRHH) |
| `POST /admin/usuarios/{username}/roles/{rol}/otorgar` | `{rol}` es `ADMIN` o `RRHH`; opcional `{ "verRolesAjenos": false }` | `UsuarioAdminResponse` |
| `POST /admin/usuarios/{username}/roles/{rol}/revocar` | — | `UsuarioAdminResponse` |
| `POST /admin/directorio/sincronizar` | — | `ResultadoSincronizacionResponse` (llama al AD; puede tardar unos segundos) |
| `GET /admin/directorio/estado` | — | `EstadoDirectorioResponse` |
| `POST /admin/directorio/buscar` | `{ "cedula": "…" }` | `PerfilDirectorioResponse[]` (una cédula puede tener varias cuentas) |
| `GET /admin/directorio/usuarios/{username}` | — | `PerfilDirectorioResponse` |

Todo usuario activo del AD es `EMPLEADO` sin necesidad de asignarlo. Los administradores de arranque (`fsepa`, `msavala`) no se pueden revocar desde la API.

## 6. Auditoría (rol `ADMIN`)

Filtros opcionales, todos en la consulta: `desde` y `hasta` (`yyyy-MM-dd`), `usuario`, `periodo`, y según el caso `resultado`, `tipo`.

| Método y ruta | Filtros | Respuesta |
|---|---|---|
| `GET /admin/auditoria/accesos` | `desde`, `hasta`, `usuario`, `resultado` (`PERMITIDO`/`DENEGADO`), `tipo` (`CONSULTA_PROPIA`/`CONSULTA_AJENA`), `periodo`, `pagina`, `tamano` | `PaginaResponse<AccesoAuditoriaResponse>` |
| `GET /admin/auditoria/accesos/exportar` | los mismos, sin paginación | `ArchivoDescargaResponse` (CSV en base64) |
| `GET /admin/auditoria/acciones` | `desde`, `hasta`, `usuario`, `tipo` (`TipoAccion`), `periodo`, `pagina`, `tamano` | `PaginaResponse<AccionAuditoriaResponse>` |
| `GET /admin/auditoria/denegados` | `desde`, `hasta`, `minimo` (por defecto 3) | `DenegadosPorUsuarioResponse[]`: quién tecleó cédulas ajenas, y cuántas distintas |

## 7. Sugerencias para el frontend

- **Ver un PDF:** convierta el base64 a `Blob` (`base64ToBlob`), muéstrelo en un `<iframe>` con una URL de objeto, y libere la URL al cerrar. No lo guarde.
- **Menús:** constrúyalos con `roles` de `mi-perfil`. Un empleado sin `puedeConsultarSuRol` no ve la consulta.
- **Carga masiva:** divida en lotes de hasta 100 archivos, súbalos en serie y acumule los `resultados` para un informe final por archivo.
- **Avisos:** después de publicar, refresque `notificaciones` cada pocos segundos hasta que `pendientes` sea 0.
- **Fechas:** vienen sin zona y ya están en hora de Ecuador; no las convierta.
- **Solo desarrollo:** las rutas `/prueba/*` no existen en producción y no forman parte del contrato.
