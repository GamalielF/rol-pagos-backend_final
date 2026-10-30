#!/bin/sh
# Borra períodos de DESARROLLO: sus avisos, sus roles, el período y los PDF de disco.
# Por defecto solo ENSAYA: muestra cuántas filas borraría y lo deshace todo.
#
#   sh scripts/borrar-periodos.sh 2026-07 2026-08             # ensayo (no borra nada)
#   sh scripts/borrar-periodos.sh --aplicar 2026-07 2026-08   # borra de verdad, con respaldo previo
#
# No toca la auditoría. Nunca lo use contra una base que no sea la de desarrollo.
set -eu
cd "$(dirname "$0")/.."

APLICAR=no
if [ "${1:-}" = "--aplicar" ]; then
  APLICAR=si
  shift
fi
[ "$#" -ge 1 ] || { echo "Indique al menos un mes con el formato aaaa-mm. Ejemplo: sh scripts/borrar-periodos.sh 2026-07" >&2; exit 1; }

LISTA=""
for mes in "$@"; do
  case "$mes" in
    [0-9][0-9][0-9][0-9]-0[1-9] | [0-9][0-9][0-9][0-9]-1[0-2]) ;;
    *) echo "Mes inválido: '$mes'. Use aaaa-mm, por ejemplo 2026-07." >&2; exit 1 ;;
  esac
  LISTA="$LISTA${LISTA:+,}'$mes-01'"
done

[ -f .env ] || { echo "No encuentro .env: ejecute el script desde la raíz del proyecto." >&2; exit 1; }
leer() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- | tr -d '\r'; }
DB_NAME=$(leer DB_NAME)
DB_USER=$(leer DB_USER)
DB_PASSWORD=$(leer DB_PASSWORD)
RUTA_PDF=$(leer ROLES_RUTA_BASE)
RUTA_PDF=${RUTA_PDF:-./data/roles-pago}
[ -n "$DB_NAME" ] && [ -n "$DB_USER" ] && [ -n "$DB_PASSWORD" ] || { echo "Faltan DB_NAME, DB_USER o DB_PASSWORD en el .env." >&2; exit 1; }

CONTENEDOR=midas-rhh-mysql
docker ps --format '{{.Names}}' | grep -qx "$CONTENEDOR" \
  || { echo "El contenedor $CONTENEDOR no está corriendo. Ejecute: sh docker/dev-up.sh" >&2; exit 1; }

mysql_bd() { docker exec -i -e MYSQL_PWD="$DB_PASSWORD" "$CONTENEDOR" mysql -u"$DB_USER" -t "$DB_NAME" "$@"; }

echo "== Períodos que coinciden =="
mysql_bd -e "SELECT p.ID, DATE_FORMAT(p.FECHA_PERIODO,'%Y-%m') AS PERIODO, p.ESTADO_PERIODO,
  (SELECT COUNT(*) FROM ROLES_PAGO r WHERE r.PERIODO_ID = p.ID) AS ROLES,
  (SELECT COUNT(*) FROM NOTIFICACIONES_ROLES_PAGO n WHERE n.PERIODO_ID = p.ID) AS AVISOS
  FROM PERIODOS p WHERE p.FECHA_PERIODO IN ($LISTA) ORDER BY p.FECHA_PERIODO"

if [ "$APLICAR" = "si" ]; then
  RESPALDO="respaldo-$(date +%Y%m%d-%H%M).sql"
  docker exec -e MYSQL_PWD="$DB_PASSWORD" "$CONTENEDOR" mysqldump -u"$DB_USER" --single-transaction "$DB_NAME" > "$RESPALDO"
  echo "== Respaldo guardado en $RESPALDO ($(wc -c < "$RESPALDO") bytes) =="
  FIN=COMMIT
else
  echo "== ENSAYO: se mostrará qué se borraría y se deshará todo =="
  FIN=ROLLBACK
fi

mysql_bd <<SQL
START TRANSACTION;
DELETE n FROM NOTIFICACIONES_ROLES_PAGO n JOIN PERIODOS p ON p.ID = n.PERIODO_ID WHERE p.FECHA_PERIODO IN ($LISTA);
SELECT ROW_COUNT() AS avisos_borrados;
DELETE r FROM ROLES_PAGO r JOIN PERIODOS p ON p.ID = r.PERIODO_ID WHERE p.FECHA_PERIODO IN ($LISTA);
SELECT ROW_COUNT() AS roles_borrados;
DELETE p FROM PERIODOS p WHERE p.FECHA_PERIODO IN ($LISTA);
SELECT ROW_COUNT() AS periodos_borrados;
$FIN;
SQL

if [ "$APLICAR" = "si" ]; then
  for mes in "$@"; do
    for carpeta in "$RUTA_PDF/$mes" "data/demo-pdfs/$mes"; do
      if [ -d "$carpeta" ]; then
        rm -rf "$carpeta"
        echo "PDF borrados: $carpeta"
      fi
    done
  done
  echo "== Listo. Recargue el front. El respaldo ($RESPALDO) tiene datos: no lo suba a Git =="
else
  echo "== Ensayo terminado: no se borró nada. Para borrar de verdad agregue --aplicar =="
fi
