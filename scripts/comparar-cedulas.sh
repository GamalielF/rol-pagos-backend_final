#!/bin/sh
# Compara las cédulas del sistema anterior con el directorio de este sistema (solo CONSULTA: no modifica nada).
# Le dice quiénes cobran en el sistema anterior pero NO están activos aquí, y por qué.
#
#   sh scripts/comparar-cedulas.sh lista-anterior.txt
#
# El archivo puede ser cualquier texto donde aparezcan las cédulas (una por línea, un CSV, o incluso el
# listado de nombres de los PDF: 0202125712.pdf). Se leen los números de 9 a 13 dígitos; con 9 se
# completa el cero inicial; los que no tienen 10 dígitos se informan aparte y no se comparan.
set -eu
cd "$(dirname "$0")/.."

ARCHIVO=${1:-}
[ -n "$ARCHIVO" ] && [ -f "$ARCHIVO" ] || { echo "Uso: sh scripts/comparar-cedulas.sh archivo-con-las-cedulas.txt" >&2; exit 1; }
[ -f .env ] || { echo "No encuentro .env: ejecute el script desde la raíz del proyecto." >&2; exit 1; }

leer() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- | tr -d '\r'; }
DB_NAME=$(leer DB_NAME)
DB_USER=$(leer DB_USER)
DB_PASSWORD=$(leer DB_PASSWORD)
[ -n "$DB_NAME" ] && [ -n "$DB_USER" ] && [ -n "$DB_PASSWORD" ] || { echo "Faltan DB_NAME, DB_USER o DB_PASSWORD en el .env." >&2; exit 1; }

CONTENEDOR=midas-rhh-mysql
docker ps --format '{{.Names}}' | grep -qx "$CONTENEDOR" \
  || { echo "El contenedor $CONTENEDOR no está corriendo. Ejecute: sh docker/dev-up.sh" >&2; exit 1; }

NUMEROS=$(tr -c '0-9\n' '\n' < "$ARCHIVO" | grep -E '^[0-9]{9,13}$' \
  | awk '{ if (length($0) == 9) print "0" $0; else print $0 }' | sort -u)
VALIDAS=$(printf '%s\n' "$NUMEROS" | grep -E '^[0-9]{10}$' || true)
DUDOSAS=$(printf '%s\n' "$NUMEROS" | grep -vE '^[0-9]{10}$' | grep -v '^$' || true)
[ -n "$VALIDAS" ] || { echo "No encontré ninguna cédula de 10 dígitos en $ARCHIVO." >&2; exit 1; }

echo "== Lectura de $ARCHIVO =="
echo "Cédulas distintas de 10 dígitos: $(printf '%s\n' "$VALIDAS" | wc -l | tr -d ' ')"
if [ -n "$DUDOSAS" ]; then
  echo "Con formato dudoso (no tienen 10 dígitos: RUC, pasaporte u otro dato), NO se comparan:"
  printf '%s\n' "$DUDOSAS" | sed 's/^/   - /'
fi

VALORES=$(printf '%s\n' "$VALIDAS" | sed "s/.*/('&')/" | paste -sd, -)

mysql_bd() { docker exec -i -e MYSQL_PWD="$DB_PASSWORD" "$CONTENEDOR" mysql -u"$DB_USER" -t "$DB_NAME" "$@"; }

mysql_bd <<SQL
CREATE TEMPORARY TABLE anterior (CEDULA VARCHAR(10) PRIMARY KEY);
INSERT INTO anterior (CEDULA) VALUES $VALORES;

SELECT 'RESUMEN' AS seccion,
       (SELECT COUNT(*) FROM anterior) AS en_el_sistema_anterior,
       (SELECT COUNT(DISTINCT CEDULA) FROM DIRECTORIO_AD WHERE ESTADO_REGISTRO = 'ACTIVO') AS personas_activas_aqui,
       (SELECT COUNT(*) FROM anterior a WHERE EXISTS
          (SELECT 1 FROM DIRECTORIO_AD d WHERE d.CEDULA = a.CEDULA AND d.ESTADO_REGISTRO = 'ACTIVO')) AS coinciden;

SELECT 'EN EL SISTEMA ANTERIOR Y NO ACTIVAS AQUI' AS seccion, a.CEDULA,
       (SELECT MAX(d.NOMBRE_COMPLETO) FROM DIRECTORIO_AD d WHERE d.CEDULA = a.CEDULA) AS nombre_en_el_directorio,
       CASE WHEN EXISTS (SELECT 1 FROM DIRECTORIO_AD d WHERE d.CEDULA = a.CEDULA AND d.ESTADO_REGISTRO = 'INACTIVO')
            THEN 'Cuenta inactiva en el AD' ELSE 'No esta en el directorio' END AS situacion
FROM anterior a
WHERE NOT EXISTS (SELECT 1 FROM DIRECTORIO_AD d WHERE d.CEDULA = a.CEDULA AND d.ESTADO_REGISTRO = 'ACTIVO')
ORDER BY situacion, a.CEDULA;

SELECT 'ACTIVAS AQUI Y AUSENTES DEL SISTEMA ANTERIOR' AS seccion, d.CEDULA,
       MAX(d.NOMBRE_COMPLETO) AS nombre, GROUP_CONCAT(d.USERNAME_AD) AS cuentas
FROM DIRECTORIO_AD d
WHERE d.ESTADO_REGISTRO = 'ACTIVO' AND d.CEDULA NOT IN (SELECT CEDULA FROM anterior)
GROUP BY d.CEDULA
ORDER BY nombre;
SQL
